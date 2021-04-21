package ru.cristalix.core.stats.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.cristalix.core.*;
import ru.cristalix.core.realm.IRealmService;
import ru.cristalix.core.stats.*;
import ru.cristalix.core.stats.impl.network.*;
import ru.cristalix.core.stats.impl.network.packet.*;
import ru.cristalix.core.stats.player.PlayerWrapper;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result.KICK_BANNED;

public class StatService implements IStatService, Listener {

	static {
		GlobalSerializers.configure(builder -> builder.registerTypeAdapter(StatServicePacketFrame.class,
				(JsonDeserializer<StatServicePacketFrame>) (jsonElement, descriptor, context) -> {
					JsonObject json = ((JsonObject) jsonElement);
					String type = json.get("type").getAsString();
					StatServicePacket data = context.deserialize(json.get("data").getAsJsonObject(), StatServicePacket.getClass(type));
					UUID uuid = json.has("uuid") ? UUID.fromString(json.get("uuid").getAsString()) : null;
					return new StatServicePacketFrame(type, data, uuid);
				}));
		GlobalSerializers.configure(builder -> builder.registerTypeAdapter(StatServicePacketFrame.class,
				(JsonSerializer<StatServicePacketFrame>) (frame, descriptor, context) -> {
					JsonObject json = new JsonObject();
					json.addProperty("type", StatServicePacket.getType(frame.getData().getClass()));
					json.add("data", context.serialize(frame.getData()));
					if (frame.getUuid() != null) json.addProperty("uuid", frame.getUuid().toString());
					return json;
				}));
	}

	private final StatsWebSocketClient client;
	private final StatServiceConnectionData connectionData;
	private final IServerPlatform platform;
	private final List<UserManager<?>> userManagers = new ArrayList<>();

	private final Cache<UUID, UUID> sessionToPlayerCache = CacheBuilder.newBuilder()
			.concurrencyLevel(4)
			.expireAfterWrite(15, TimeUnit.SECONDS)
			.build();

	private final Cache<UUID, UUID> playerToSessionCache = CacheBuilder.newBuilder()
			.concurrencyLevel(4)
			.expireAfterWrite(15, TimeUnit.SECONDS)
			.build();

	private final Map<UUID, Player> sessionToPlayerMap = new HashMap<>();
	private final Map<UUID, UUID> playerToSessionMap = new HashMap<>();
	private final Logger logger;
	private final String realm;

	@Getter
	@Setter
	private boolean dataRequired;

	private final IPlatformEventExecutor<Object, Object, Object> eventExecutor;

	public StatService(IServerPlatform platform, StatServiceConnectionData data) {
		this.logger = Logger.getLogger("StatService");
		this.client = new StatsWebSocketClient(this.logger, platform.getScheduler());
		//		this.client.setDebugReads(true);
		//		this.client.setDebugWrites(true);
		this.connectionData = data;
		this.platform = platform;
		this.eventExecutor = platform.getPlatformEventExecutor();
		this.realm = IRealmService.get().getCurrentRealmInfo().getRealmId().getRealmName();
	}

	@Override
	@SneakyThrows
	public void useScopes(Scope<?>... scopes) {
		List<String> scopeIds = this.client.getScopes();
		for (Scope<?> scope : scopes) {
			scopeIds.add(scope.getInternalId());
		}
		if (this.client.isActive())
			this.client.send(new PacketUseScopes(scopeIds)).await(PacketOk.class);
	}

	@Override
	public <T> UserManager<T> registerUserManager(UserProvider<T> provider, UserSerializer<T> serializer) {
		UserManager<T> manager = new UserManagerImpl<>(provider, serializer);
		this.userManagers.add(manager);
		return manager;
	}

	@Override
	@SuppressWarnings ({"unchecked", "rawtypes"})
	public StatContext saveUser(UUID uuid) {
		StatContext context = new StatContextImpl(uuid, null, new HashMap<>());
		for (UserManager userManager : userManagers) {
			userManager.serialize(userManager.getUser(uuid), context);
		}
		return context;
	}

	@Override
	public void enable() {

		client.connect(connectionData);
		//		client.waitForHandshake();

		client.addListener(PacketRequestSync.class, this::onSyncRequest);

		eventExecutor.registerListener(AsyncPlayerPreLoginEvent.class, this, this::onPreLogin, EventPriority.LOW, false);
		eventExecutor.registerListener(PlayerLoginEvent.class, this, this::onLogin, EventPriority.MONITOR, false);
		eventExecutor.registerListener(PlayerJoinEvent.class, this, this::onJoin, EventPriority.NORMAL, false);
		eventExecutor.registerListener(PlayerQuitEvent.class, this, this::onQuit, EventPriority.HIGHEST, false);

		platform.getScheduler().runSyncRepeating(() -> {
			if (client.isActive()) client.send(new PacketKeepAlive());
		}, 5, TimeUnit.SECONDS);

	}

	@Override
	public void disable() {

		Bukkit.getOnlinePlayers().forEach(this::finalSave);

	}

	private void onLogin(PlayerLoginEvent event) {

		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		UUID session = playerToSessionCache.getIfPresent(uuid);
		if (session == null) {
			logger.severe("No session found for " + uuid + " " + player.getName());
			event.disallow(PlayerLoginEvent.Result.KICK_BANNED, "Вход на сервер занял слишком много времени");
			return;
		}

		sessionToPlayerMap.put(session, player);
		playerToSessionMap.put(uuid, session);

	}

	@Override
	public <T> CompletableFuture<List<T>> getLeaderboard(Scope<T> scope, String field, int limit) {
		Dialogue dialogue = client.send(new PacketRequestLeaderboard(scope.getInternalId(), field, limit));
		return dialogue.awaitFuture(PacketLeaderboardState.class).thenApply(packet -> Arrays.stream(packet.getEntries())
				.map(entry -> GlobalSerializers.fromJson(entry, scope.getType())).collect(Collectors.toList()));
	}

	private void onJoin(PlayerJoinEvent event) {

		Player player = event.getPlayer();
		for (UserManager<?> userManager : userManagers) {
			Object user = userManager.getUser(player);
			if (user instanceof PlayerWrapper) {
				((PlayerWrapper) user).setPlayer(player);
			}
		}

	}

	public Map<String, JsonElement> takeDataSnapshot(UUID userUuid) {
		StatContextImpl statContext = new StatContextImpl(userUuid, null, new HashMap<>());
		for (UserManager<?> userManager : userManagers) {
			writeUser(userManager, userUuid, statContext);
		}
		return statContext.getDictionary();
	}

	private static <T> void writeUser(UserManager<T> manager, UUID uuid, StatContext writer) {
		T user = manager.getUser(uuid);
		manager.serialize(user, writer);
	}

	private static <T> void readAndAddUser(UserManager<T> manager, StatContext userData) {
		T user = manager.createUser(userData);
		manager.addUser(userData.getUuid(), user);
	}

	private void onSyncRequest(Dialogue dialogue, PacketRequestSync request) {

		UUID session = request.getSession();
		Player player = sessionToPlayerMap.get(session);

		if (player == null) {
			dialogue.send(new PacketError(PacketError.ErrorLevel.WARNING, "Nothing to sync yet"));
			return;
		}

		StatContext stats;
		try {
			stats = this.saveUser(player.getUniqueId());
		} catch (Exception exception) {
			dialogue.send(new PacketError(PacketError.ErrorLevel.SEVERE, "Sync failed"));
			return;
		}

		dialogue.send(new PacketSyncData(session, stats.getDictionary()));

	}

	private void onQuit(PlayerQuitEvent event) {

		finalSave(event.getPlayer());

	}

	private void finalSave(Player player) {
		UUID uuid = player.getUniqueId();

		UUID session = playerToSessionMap.remove(uuid);
		if (session == null) session = playerToSessionCache.getIfPresent(uuid);

		if (session == null) {
			logger.warning("Weird: player " + player.getName() + " disconnected without having a session");
			return;

		}

		PacketSyncData dataPacket = new PacketSyncData(session, takeDataSnapshot(uuid));
		client.send(dataPacket);

		for (UserManager<?> userManager : userManagers) {
			// ToDo: When player rejoins the server (dynamic realms), this code will create a race
			userManager.removeUser(uuid);
		}

		sessionToPlayerMap.remove(session);
		client.send(new PacketEndSession(session));
	}

	private void onPreLogin(AsyncPlayerPreLoginEvent event) {
		if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;
		UUID uuid = event.getUniqueId();
		String name = event.getName();

		StatContextImpl reader;
		UUID session = UUID.randomUUID();

		long started = System.currentTimeMillis();

		Map<String, JsonElement> stats = null;

		boolean failed = false;

		try {

			PacketCreateSession connectPacket = new PacketCreateSession(uuid, session, name, realm);
			stats = client.send(connectPacket).await(PacketSyncData.class).getStats();

		} catch (StatServiceException e) {

			failed = true;
			e.printStackTrace();

			try {
				client.send(new PacketEndSession(session));
			} catch (Exception ignored) {}

			if (dataRequired) {
				event.setLoginResult(KICK_BANNED);
				event.setKickMessage("§cБаза данных походу наелась и спит, попробуй зайти ещё раз, может проснётся");
				return;
			}
		}

		if (stats == null) stats = new HashMap<>();

		if (!failed) {
			this.sessionToPlayerCache.put(session, uuid);
		}
		reader = new StatContextImpl(uuid, name, stats);

		System.out.println("Loaded " + name + " in " + (System.currentTimeMillis() - started) + "ms.");

		CompletableFuture<Void> loadedFlag = new CompletableFuture<>();

		boolean finalFailed = failed;
		platform.getScheduler().runSync(() -> {
			try {
				if (loadedFlag.isDone()) return; // Server lagged out
				for (UserManager<?> userManager : userManagers) {
					readAndAddUser(userManager, reader);
				}
				if (!finalFailed) {
					playerToSessionCache.put(uuid, session);
				}
				loadedFlag.complete(null);
			} catch (Throwable throwable) {
				for (UserManager<?> userManager : userManagers) {
					userManager.removeUser(uuid);
				}
				loadedFlag.completeExceptionally(throwable);
			}
		});

		try {
			loadedFlag.get(5, TimeUnit.SECONDS);
		} catch (InterruptedException | TimeoutException e) {
			event.disallow(KICK_BANNED, "§cСервер немного подвис, попробуй зайти ещё раз");
			e.printStackTrace();
		} catch (ExecutionException e) {
			event.setLoginResult(KICK_BANNED);
			event.setKickMessage("§cПри загрузке твоих данных произошла ошибка! Срочно беги к разработчику режима!");
			e.printStackTrace();
		}

	}

}
