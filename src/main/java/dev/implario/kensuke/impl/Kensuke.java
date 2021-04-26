package dev.implario.kensuke.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.*;
import dev.implario.kensuke.*;
import dev.implario.kensuke.impl.network.*;
import dev.implario.kensuke.impl.network.packet.*;
import dev.implario.kensuke.player.PlayerWrapper;
import dev.implario.kensuke.scope.Scope;
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
import ru.cristalix.core.GlobalSerializers;
import ru.cristalix.core.IPlatformEventExecutor;
import ru.cristalix.core.IServerPlatform;
import ru.cristalix.core.account.IAccountService;
import ru.cristalix.core.realm.IRealmService;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result.KICK_BANNED;

public class Kensuke implements IKensuke, Listener {

	static {
		GlobalSerializers.configure(builder -> builder.registerTypeAdapter(KensukePacketFrame.class,
				(JsonDeserializer<KensukePacketFrame>) (jsonElement, descriptor, context) -> {
					JsonObject json = ((JsonObject) jsonElement);
					String type = json.get("type").getAsString();
					KensukePacket data = context.deserialize(json.get("data").getAsJsonObject(), KensukePacket.getClass(type));
					UUID uuid = json.has("uuid") ? UUID.fromString(json.get("uuid").getAsString()) : null;
					return new KensukePacketFrame(type, data, uuid);
				}));
		GlobalSerializers.configure(builder -> builder.registerTypeAdapter(KensukePacketFrame.class,
				(JsonSerializer<KensukePacketFrame>) (frame, descriptor, context) -> {
					JsonObject json = new JsonObject();
					json.addProperty("type", KensukePacket.getType(frame.getData().getClass()));
					json.add("data", context.serialize(frame.getData()));
					if (frame.getUuid() != null) json.addProperty("uuid", frame.getUuid().toString());
					return json;
				}));
		GlobalSerializers.configure(builder -> builder.registerTypeAdapter(Scope.class,
				(JsonSerializer<Scope<?>>) (scope, descriptor, context) ->
						new JsonPrimitive(scope.getInternalId())));
	}

	private final KensukeWebSocketClient client;
	private final KensukeConnectionData connectionData;
	private final IServerPlatform platform;
	private final List<UserPool<?>> userManagers = new ArrayList<>();
	@Getter
	private List<Scope<?>> scopes = new ArrayList<>();

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

	public Kensuke(IServerPlatform platform, KensukeConnectionData connectionData) {
		logger = Logger.getLogger("Kensuke");
		client = new KensukeWebSocketClient(logger, platform.getScheduler());
		this.connectionData = connectionData;
		this.platform = platform;
		eventExecutor = platform.getPlatformEventExecutor();
		realm = IRealmService.get().getCurrentRealmInfo().getRealmId().getRealmName();
	}

	@Override
	@SneakyThrows
	public void useScopes(Scope<?>... scopes) {
		this.scopes = Arrays.asList(scopes);
		List<String> scopeIds = client.getScopes();
		for (Scope<?> scope : scopes) {
			scopeIds.add(scope.getInternalId());
		}
		if (client.isActive()) {
			client.send(new PacketUseScopes(scopeIds)).await(PacketOk.class);
		}
	}

	@Override
	public <T> UserPool<T> registerUserManager(DataDeserializer<T> provider, DataSerializer<T> serializer, Scope<?>... scopes) {
		UserPool<T> manager = new UserPoolImpl<>(provider, serializer, Arrays.asList(scopes));
		userManagers.add(manager);
		return manager;
	}

	@Override
	@SuppressWarnings ({"unchecked", "rawtypes"})
	public DataContext saveUser(UUID uuid) {
		DataContext context = new DataContextImpl(uuid, null, new HashMap<>());
		for (UserPool userManager : userManagers) {
			userManager.serialize(userManager.getUser(uuid), context);
		}
		return context;
	}

	@Override
	public void enable() {
		client.connect(connectionData);
		//client.waitForHandshake();

		client.addListener(PacketRequestSync.class, this::onSyncRequest);

		eventExecutor.registerListener(AsyncPlayerPreLoginEvent.class, this, this::onPreLogin, EventPriority.LOW, false);
		eventExecutor.registerListener(PlayerLoginEvent.class, this, this::onLogin, EventPriority.MONITOR, false);
		eventExecutor.registerListener(PlayerJoinEvent.class, this, this::onJoin, EventPriority.NORMAL, false);
		eventExecutor.registerListener(PlayerQuitEvent.class, this, this::onQuit, EventPriority.HIGHEST, false);

		platform.getScheduler().runSyncRepeating(() -> {
			if (client.isActive()) {
				client.send(new PacketKeepAlive());
			}
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
		return dialogue.awaitFuture(PacketLeaderboardState.class)
				.thenApply(packet -> Arrays.stream(packet.getEntries())
				.map(entry -> GlobalSerializers.fromJson(entry, scope.getType()))
				.collect(Collectors.toList()));
	}

	@Override
	public <T> CompletableFuture<List<T>> addLeaderboardUsers(UserPool<T> userManager, Scope<T> scope, String field, int limit) {
		Dialogue dialogue = client.send(new PacketRequestLeaderboard(scope.getInternalId(), field, limit));
		return dialogue.awaitFuture(PacketLeaderboardState.class)
				.thenApply(packet -> Arrays.stream(packet.getEntries())
				.map(entry -> {
					val uuid = UUID.fromString(entry.get("id").getAsString());
					return userManager.getUserCache().computeIfAbsent(uuid, uuid1 -> {
						try {
							T user = IAccountService.get().getNameByUuid(uuid1).thenApply(name -> {
								DataContext context = new DataContextImpl(uuid1, name, Collections.singletonMap(scope.getInternalId(), entry));
								return userManager.createUser(context);
							}).get();
							if (userManager.isUserLoaded(uuid)) {
								userManager.addUser(uuid, user);
							}
							return user;
						} catch (InterruptedException | ExecutionException exception) {
							logger.log(Level.WARNING, "Unable to parse leaderboard response.", exception);
							return null;
						}
					});
				}).collect(Collectors.toList()));
	}

	@Override
	public CompletableFuture<DataContext> loadSnapshot(UUID uuid, List<Scope<?>> scopes) {
		Dialogue dialogue = client.send(new PacketRequestSnapshot(uuid, scopes));
		return dialogue.awaitFuture(PacketDataSnapshot.class)
				.thenApply(packet -> new DataContextImpl(packet.getId(), packet.getName(), packet.getStats()));
	}

	private void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		for (UserPool<?> userManager : userManagers) {
			Object user = userManager.getUser(player);
			if (user instanceof PlayerWrapper) {
				((PlayerWrapper) user).setPlayer(player);
			}
		}
	}

	public Map<String, JsonElement> takeDataSnapshot(UUID userUuid) {
		DataContextImpl context = new DataContextImpl(userUuid, null, new HashMap<>());
		for (UserPool<?> userManager : userManagers) {
			writeUser(userManager, userUuid, context);
		}
		return context.getDictionary();
	}

	private static <T> void writeUser(UserPool<T> manager, UUID uuid, DataContext writer) {
		T user = manager.getUser(uuid);
		manager.serialize(user, writer);
	}

	private static <T> void readAndAddUser(UserPool<T> manager, DataContext userData) {
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

		DataContext context;
		try {
			context = saveUser(player.getUniqueId());
		} catch (Exception exception) {
			dialogue.send(new PacketError(PacketError.ErrorLevel.SEVERE, "Sync failed"));
			return;
		}

		dialogue.send(new PacketSyncData(session, context.getDictionary()));
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

		for (UserPool<?> userManager : userManagers) {
			// ToDo: When player rejoins the server (dynamic realms), this code will create a race
			userManager.removeUser(uuid);
		}

		sessionToPlayerMap.remove(session);
		client.send(new PacketEndSession(session));
	}

	private void onPreLogin(AsyncPlayerPreLoginEvent event) {
		if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
			return;
		}

		UUID uuid = event.getUniqueId();
		String name = event.getName();

		DataContextImpl reader;
		UUID session = UUID.randomUUID();

		long started = System.currentTimeMillis();

		Map<String, JsonElement> data = null;

		boolean failed = false;

		try {
			PacketCreateSession connectPacket = new PacketCreateSession(uuid, session, name, realm);
			data = client.send(connectPacket).await(PacketSyncData.class).getStats();
		} catch (KensukeException exception) {
			exception.printStackTrace();
			failed = true;

			try {
				client.send(new PacketEndSession(session));
			} catch (Exception ignored) {}

			if (dataRequired) {
				event.setLoginResult(KICK_BANNED);
				event.setKickMessage("§cБаза данных походу наелась и спит, попробуй зайти ещё раз, может проснётся");
				return;
			}
		}

		if (data == null) {
			data = new HashMap<>();
		}

		if (!failed) {
			sessionToPlayerCache.put(session, uuid);
		}
		reader = new DataContextImpl(uuid, name, data);

		System.out.println("Loaded " + name + " in " + (System.currentTimeMillis() - started) + "ms.");

		CompletableFuture<Void> loadedFlag = new CompletableFuture<>();

		boolean finalFailed = failed;
		platform.getScheduler().runSync(() -> {
			try {
				if (loadedFlag.isDone()) {
					return; // Server lagged out
				}
				for (UserPool<?> userManager : userManagers) {
					readAndAddUser(userManager, reader);
				}
				if (!finalFailed) {
					playerToSessionCache.put(uuid, session);
				}
				loadedFlag.complete(null);
			} catch (Throwable throwable) {
				for (UserPool<?> userManager : userManagers) {
					userManager.removeUser(uuid);
				}
				loadedFlag.completeExceptionally(throwable);
			}
		});

		try {
			loadedFlag.get(5, TimeUnit.SECONDS);
		} catch (InterruptedException | TimeoutException exception) {
			exception.printStackTrace();
			event.disallow(KICK_BANNED, "§cСервер немного подвис, попробуй зайти ещё раз");
		} catch (ExecutionException exception) {
			exception.printStackTrace();
			event.disallow(KICK_BANNED, "§cПри загрузке твоих данных произошла ошибка! Срочно беги к разработчику режима!");
		}
	}

}
