package ru.cristalix.core.stats.impl.network;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;
import lombok.*;
import ru.cristalix.core.GlobalSerializers;
import ru.cristalix.core.IPlatformScheduler;
import ru.cristalix.core.LinkedRunnable;
import ru.cristalix.core.stats.impl.network.packet.PacketAuth;
import ru.cristalix.core.stats.impl.network.packet.PacketError;
import ru.cristalix.core.stats.impl.network.packet.PacketOk;
import ru.cristalix.core.stats.impl.network.packet.PacketUseScopes;

import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequiredArgsConstructor
@ChannelHandler.Sharable
@SuppressWarnings ({"rawtypes", "unchecked"})
public class StatsWebSocketClient extends SimpleChannelInboundHandler<WebSocketFrame> {

	private static final LinkedRunnable NOOP_RUNNABLE = () -> {};
	private static final int MAX_CONTEXT_LENGTH = Integer.getInteger("cristalix.core.net-context-limit", 65536);
	private static final Class<? extends SocketChannel> CHANNEL_CLASS;
	private static final EventLoopGroup GROUP;
	private final Object handshakeLock = new Object();

	@Getter
	private final Cache<UUID, CompletableFuture> responseCache = CacheBuilder.newBuilder()
			.concurrencyLevel(3)
			.expireAfterWrite(10L, TimeUnit.SECONDS)
			.<UUID, CompletableFuture>removalListener(notification -> {
				if (notification.getCause() == RemovalCause.EXPIRED) {
					val callback = notification.getValue();
					if (!callback.isDone()) {
						callback.completeExceptionally(new TimeoutException("Packet " + notification.getKey() + " timed out"));
					}
				}
			})
			.build();

	//	private final Queue<Runnable> commandQueue = new ConcurrentLinkedQueue<>();

	private final Deque<StatServicePacketFrame> commandQueue = new ConcurrentLinkedDeque<>();

	private final Multimap<Class<? extends StatServicePacket>, BiConsumer<Dialogue, StatServicePacket>> listenerMap = HashMultimap.create();

	private final Logger logger;
	private final IPlatformScheduler<?> scheduler;

	private Channel channel;

	@Getter
	@Setter
	private boolean autoReconnect = true;

	private boolean handshaked;

	@Setter
	private boolean debugReads, debugWrites;

	private StatServiceConnectionData connectionData;

	@Getter
	private final List<String> scopes = new ArrayList<>();

	public ChannelFuture connect(StatServiceConnectionData connectionData) {
		this.connectionData = connectionData;
		return connectImpl();
	}

	public Dialogue send(StatServicePacket packet) {
		UUID uuid = UUID.randomUUID();
		DialogueImpl dialogue = new DialogueImpl(uuid, this);
		return dialogue.send(packet);
	}

	@SneakyThrows
	public void waitForHandshake() {
		synchronized (handshakeLock) {
			while (!handshaked) {
				handshakeLock.wait();
			}
		}
	}

	public void write(StatServicePacketFrame frame) {
		if (!isActive()) {
			queuePacket(frame);
			return;
		}
		checkEventLoopAndEnqueue(() -> {
			flushPacketQueue(false);
			dispatchPacket(frame);
		});
	}

	public void close() {
		if (isConnected()) {
			closeImpl();
		}
	}

	public boolean isConnected() {
		return channel != null && channel.isOpen();
	}

	public boolean isActive() {
		return isConnected() && handshaked;
	}

	@SuppressWarnings ("unchecked")
	public <T extends StatServicePacket> void addListener(Class<T> clazz, BiConsumer<Dialogue, T> listener) {
		validateListener(clazz, listener);
		listenerMap.put(clazz, (BiConsumer<Dialogue, StatServicePacket>) listener);
	}

	public <T extends StatServicePacketFrame> void removeListener(Class<T> clazz, BiConsumer<Dialogue, T> listener) {
		validateListener(clazz, listener);
		listenerMap.remove(clazz, listener);
	}

	public void schedule(Runnable command, long delay, TimeUnit unit) {
		GROUP.schedule(command, delay, unit);
	}

	public void writeBatch(StatServicePacketFrame... packages) {
		writeBatch(Arrays.asList(packages));
	}

	public void writeBatch(Collection<StatServicePacketFrame> packets) {
		if (packets.isEmpty()) return;
		if (!isActive()) {
			packets.forEach(this::queuePacket);
			return;
		}
		checkEventLoopAndEnqueue(() -> {
			int size = packets.size();
			val ch = channel;
			val it = packets.iterator();
			for (int i = 0; i < size; i++) {
				ch.write(it.next(), ch.voidPromise());
			}
			ch.flush();
		});
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
		if (evt == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE) {
			sendHandshake();
		}
	}

	@Override
	public boolean acceptInboundMessage(Object msg) {
		return msg instanceof TextWebSocketFrame || msg instanceof BinaryWebSocketFrame;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msg) {
		if (msg instanceof TextWebSocketFrame) {
			readTextFrame((TextWebSocketFrame) msg);
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		closeImpl();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		logger.log(Level.SEVERE, "StatService exception", cause);
	}

	private void processAutoReconnect() {
		if (autoReconnect) {
			logger.warning("Automatically reconnecting in next 1.5 seconds");
			// Schedule operation to not consume CPU
			schedule(this::connectImpl, 1500L, TimeUnit.MILLISECONDS);
		}
	}

	private void sendHandshake() {
		val greetingPackage = new PacketAuth(connectionData.getLogin(), connectionData.getPassword(), connectionData.getNodeName());
		StatServicePacketFrame frame = toStatServiceFrame(greetingPackage);
		CompletableFuture<StatServicePacket> authFuture = new CompletableFuture<>();
		responseCache.put(frame.getUuid(), authFuture);
		authFuture.thenAccept(packet -> {
			if (packet instanceof PacketError) {
				channel.close();
				PacketError error = (PacketError) packet;
				logger.severe("Unable to authorize: " + error);
			} else if (packet instanceof PacketOk) {

				channel.writeAndFlush(toWebSocketFrame(toStatServiceFrame(new PacketUseScopes(scopes))));

				logger.info("Successful auth: " + packet);
				flushPacketQueue(true);
				handshaked = true;
				synchronized (handshakeLock) {
					handshakeLock.notifyAll();
				}
			}
		});

		channel.writeAndFlush(toWebSocketFrame(frame))
				.addListeners(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE, (ChannelFutureListener) future -> {
					if (!future.isSuccess()) {
						logger.log(Level.SEVERE, "Error during handshake", future.cause());
						future.channel().close();
					}
				});
	}

	private void queuePacket(StatServicePacketFrame frame) {
		if (!commandQueue.offerLast(frame)) {
			throw new AssertionError("Failed to offer command!");
		}
	}

	private void flushPacketQueue(boolean flushChannel) {

		if (commandQueue.isEmpty()) return;

		StatServicePacketFrame packet;
		while ((packet = commandQueue.pollFirst()) != null) {
			this.channel.write(packet, channel.voidPromise());
		}
		if (flushChannel) channel.flush();

	}

	private void closeImpl() {
		val channel = this.channel;
		if (channel == null) return;
		logger.info("Client shutdown");
		channel.close();
		this.channel = null;
		//		CoreApi.get().bus().post(new TowerEvent.Disconnected(this));
		commandQueue.clear();
		responseCache.invalidateAll();
		processAutoReconnect();
	}

	private void readTextFrame(TextWebSocketFrame webSocketFrame) {
		val text = webSocketFrame.text();
		if (debugReads) {
			logger.warning("IN » " + text);
		}
		val frame = GlobalSerializers.fromJson(text, StatServicePacketFrame.class);
		val clazz = StatServicePacket.getClass(frame.getType());
		if (clazz == null) {
			logger.warning("Unable to resolve class for '" + frame.getType() + "' packet type");
			return;
		}

		handlePacket(frame);
	}

	@SuppressWarnings ("unchecked")
	private void handlePacket(StatServicePacketFrame packet) {
		LinkedRunnable runnable = NOOP_RUNNABLE;
		val id = packet.getUuid();
		val data = packet.getData();
		if (id != null) {
			val callback = responseCache.getIfPresent(id);
			if (callback != null && !callback.isDone()) {
				runnable = runnable.andThen(() -> callback.complete(data));
			}
		}
		val clazz = packet.getData().getClass();
		val listeners = listenerMap.get(clazz);
		if (!listeners.isEmpty()) {
			runnable = runnable.andThen(() -> notifyListeners(listeners, packet));
		}
		if (runnable != NOOP_RUNNABLE) {
			scheduler.runSync(runnable);
		}
	}

	private void notifyListeners(Collection<BiConsumer<Dialogue, StatServicePacket>> listeners, StatServicePacketFrame frame) {
		DialogueImpl dialogue = new DialogueImpl(frame.getUuid(), this);
		listeners.forEach(listener -> listener.accept(dialogue, frame.getData()));
	}

	private ChannelFuture connectImpl() {
		handshaked = false;
		return new Bootstrap()
				.channel(CHANNEL_CLASS)
				.group(GROUP)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
				.handler(new ChannelInitializer<Channel>() {
					@Override
					protected void initChannel(Channel ch) {
						val config = ch.config();
						config.setOption(ChannelOption.IP_TOS, 24);
						config.setAllocator(PooledByteBufAllocator.DEFAULT);
						config.setOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);
						config.setOption(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
						ch.pipeline()
								.addLast("codec", new HttpClientCodec())
								.addLast("aggregator", new HttpObjectAggregator(MAX_CONTEXT_LENGTH))
								.addLast("protocol_handler", new WebSocketClientProtocolHandler(
										WebSocketClientHandshakerFactory.newHandshaker(
												URI.create("ws://" + connectionData.getHost() + ":" + connectionData.getPort() + "/"),
												WebSocketVersion.V13,
												null,
												false,
												new DefaultHttpHeaders(),
												MAX_CONTEXT_LENGTH
																					  ),
										true
								))
								.addLast("handler_boss", StatsWebSocketClient.this);
					}
				})
				.remoteAddress(connectionData.getHost(), connectionData.getPort())
				.connect().addListener((ChannelFutureListener) future -> {
					if (future.isSuccess()) {
						logger.info("Connection succeeded, bound to: " + (channel = future.channel()));
					} else {
						logger.log(Level.SEVERE, "Connection failed", future.cause());
						processAutoReconnect();
					}
				});
	}

	private StatServicePacketFrame toStatServiceFrame(StatServicePacket packet) {
		return new StatServicePacketFrame(StatServicePacket.getType(packet.getClass()), packet, UUID.randomUUID());
	}

	private WebSocketFrame toWebSocketFrame(StatServicePacketFrame statServiceFrame) {
		val json = GlobalSerializers.toJson(statServiceFrame);
		if (debugWrites) {
			logger.warning("OUT » " + json);
		}
		return new TextWebSocketFrame(json);
	}

	private void checkEventLoopAndEnqueue(Runnable command) {
		val eventLoop = channel.eventLoop();
		if (eventLoop.inEventLoop()) {
			command.run();
		} else {
			eventLoop.execute(command);
		}
	}

	private void dispatchPacket(StatServicePacketFrame packet) {
		dispatchFrame(toWebSocketFrame(packet));
	}

	private void dispatchFrame(WebSocketFrame frame) {
		val channel = this.channel;
		channel.writeAndFlush(frame, channel.voidPromise());
	}

	@SuppressWarnings ("rawtypes")
	private static void validateListener(Class clazz, BiConsumer listener) {
		Objects.requireNonNull(clazz, "clazz");
		Objects.requireNonNull(listener, "listener");
	}

	static {
		boolean epoll;
		try {
			Class.forName("io.netty.channel.epoll.Epoll");
			epoll = !Boolean.getBoolean("cristalix.net.disable-native-transport") && Epoll.isAvailable();
		} catch (ClassNotFoundException ignored) {
			epoll = false;
		}
		if (epoll) {
			CHANNEL_CLASS = EpollSocketChannel.class;
			GROUP = new EpollEventLoopGroup(1);
		} else {
			CHANNEL_CLASS = NioSocketChannel.class;
			GROUP = new NioEventLoopGroup(1);
		}
	}

	@RequiredArgsConstructor
	private class 	PacketWriteTask implements Runnable {

		final StatServicePacketFrame packet;

		public void run() {
			channel.write(this.packet, channel.voidPromise());
		}

	}

}
