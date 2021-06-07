package dev.implario.kensuke.impl.bukkit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.implario.kensuke.*;
import dev.implario.kensuke.impl.DataContextImpl;
import dev.implario.kensuke.impl.KensukeException;
import dev.implario.kensuke.impl.KensukeImpl;
import dev.implario.kensuke.Session;
import dev.implario.kensuke.impl.packet.*;
import dev.implario.nettier.RemoteException;
import dev.implario.nettier.Talk;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result.KICK_BANNED;

@RequiredArgsConstructor
public class BukkitKensukeAdapter implements Listener {

    private final KensukeImpl kensuke;
    private final Plugin plugin;

    // Due to lack of better option to distinguish players in pre-login phase, we are forced to use this hack.
    private final Cache<IdentityUUID, Session> loadingPlayers = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .expireAfterWrite(15, TimeUnit.SECONDS)
            .build();

    private final Map<Session, Player> sessionToPlayerMap = new HashMap<>();
    private final Map<Player, Session> playerToSessionMap = new HashMap<>();

    private final Map<UUID, Session> sessionMap = new ConcurrentHashMap<>();

//        platform.getScheduler().runSyncRepeating(() -> {
//            if (client.isActive()) {
//     todo           client.send(new PacketKeepAlive());
//            }
//        }, 5, TimeUnit.SECONDS);

// todo        Bukkit.getOnlinePlayers().forEach(this::finalSave);

    public void init() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            long time = System.currentTimeMillis();
            for (Session session : sessionMap.values()) {
                if (session.getLastSave() + 60000 > time) continue;

                PacketSyncData dataPacket = new PacketSyncData(session.getSessionId(), createSave(session).getDictionary());

                kensuke.getClient().send(dataPacket).awaitFuture(PacketOk.class).thenAccept(f -> {
                    Session key = sessionMap.get(session.getSessionId());
                    if (key != null) {
                        Player player = sessionToPlayerMap.get(key);
                        if (player != null) {
                            player.sendMessage("§aВаши данные сохранены: §f" + f.getMessage());
                        }
                    }
                    System.out.println("autosave: " + session.getUserId() + ", " + f.getMessage());
                });

                session.setLastSave(time);
            }
        }, 1, 1);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        System.out.println(loadingPlayers.size() + " players in loading cache");
        Session session = loadingPlayers.getIfPresent(new IdentityUUID(event.getPlayer().getUniqueId()));
        if (session == null) {
            kensuke.getLogger().warning("No session found for " + uuid + " (" + player.getName() + ") during login");
//            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, "Kensuke: Вход на сервер занял слишком много времени");
            return;
        }

        sessionToPlayerMap.put(session, player);
        playerToSessionMap.put(player, session);
    }


    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Session session = playerToSessionMap.get(player);

        if (session == null) {
            event.getPlayer().sendMessage("§cНам не удалось прогрузить ваши данные,");
            event.getPlayer().sendMessage("§cТекущая сессия не попадёт в статистику.");
            return;
        }

        for (val entry : session.getUserObjects().entrySet()) {

            entry.add();
            IKensukeUser user = entry.getUser();

            if (user instanceof IBukkitKensukeUser) {
                ((IBukkitKensukeUser) user).setPlayer(player);
            }

        }
    }

    public DataContext createSave(Session session) {
        DataContextImpl dataContext = new DataContextImpl(kensuke.getGson(), session.getUserId().toString(), new HashMap<>());
        session.createSave(dataContext);
        return dataContext;
    }

    private void onSyncRequest(Talk talk, PacketRequestSync request) {
        UUID sessionId = request.getSession();
        Session session = sessionMap.get(sessionId);
        if (session == null) {
            talk.respond(new PacketError(RemoteException.ErrorLevel.SEVERE, "No such session"));
            return;
        }

        Player player = sessionToPlayerMap.get(session);

        if (player == null) {
            talk.respond(new PacketError(RemoteException.ErrorLevel.WARNING, "Nothing to sync yet"));
            return;
        }

        DataContext context;
        try {
            context = createSave(session);
        } catch (Exception exception) {
            kensuke.getLogger().severe("Error while saving " + player.getUniqueId() + " (" + player.getName() + "):");
            exception.printStackTrace();
            talk.respond(new PacketError(RemoteException.ErrorLevel.SEVERE, "Sync failed"));
            return;
        }

        talk.respond(new PacketSyncData(sessionId, context.getDictionary()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        Session session = playerToSessionMap.remove(player);

        if (session == null) {

            Session cachedSession = loadingPlayers.getIfPresent(new IdentityUUID(player.getUniqueId()));
            if (cachedSession != null) {
                kensuke.getLogger().warning("Player " + player.getUniqueId() + " (" + player.getName() + ") was tried to be saved during login");
                kensuke.getClient().send(cachedSession);
            } else {
                kensuke.getLogger().warning("Player " + player.getUniqueId() + " (" + player.getName() + ") disconnected without having a session");
            }

            return;
        }

        PacketSyncData dataPacket = new PacketSyncData(session.getSessionId(), createSave(session).getDictionary());
        kensuke.getClient().send(dataPacket);

        for (UserObjectsMap.Entry<?> entry : session.getUserObjects()) {
            entry.remove();
        }

        sessionToPlayerMap.remove(session);
        playerToSessionMap.remove(player);
        sessionMap.remove(session.getSessionId());
        kensuke.getClient().send(new PacketEndSession(session.getSessionId()));
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {

        try {
            handlePreLogin(event);
        } catch (Exception exception) {
            exception.printStackTrace();
            event.disallow(KICK_BANNED, exception.getMessage());
        }

    }

    private void handlePreLogin(AsyncPlayerPreLoginEvent event) throws Exception {

        // Do not load data if login is already forbidden
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }

        UUID userId = event.getUniqueId();

        UUID sessionId = UUID.randomUUID();
        Session session = new Session(sessionId, userId);

        UserLoadEvent loadEvent = new UserLoadEvent(event, kensuke.getGlobalRealm());

        loadEvent.getUserManagers().addAll(kensuke.getGlobalUserManagers());

        Bukkit.getPluginManager().callEvent(loadEvent);

        boolean dataRequired = loadEvent.getUserManagers().stream().anyMatch(m -> !m.isOptional());

        Set<Scope<?>> scopes = loadEvent.getUserManagers().stream()
                .flatMap(m -> m.getScopes().stream())
                .collect(Collectors.toSet());

        // If nobody is using kensuke, then no session will be created.
        // ToDo: Subsequent events are handling this case as an error
        if (scopes.isEmpty()) return;

        String realm = loadEvent.getRealm();

        // Disallow logins that do lock some scopes, but didn't receive a realm
        if (realm == null) {
            throw new IllegalStateException("No global realm defined and no one bothered to claim you");
        }

        DataContext context;

        try {
            context = kensuke.createSession(sessionId, userId.toString(), realm, scopes.toArray(new Scope[0])).get(1, TimeUnit.SECONDS);

            loadingPlayers.put(new IdentityUUID(event.getUniqueId()), session);
            System.out.println(loadingPlayers.size() + " players cached prelogin");
        } catch (KensukeException exception) {
            kensuke.getLogger().log(Level.SEVERE, "Error while fetching data for " + userId + " (" + event.getName() + ")", exception);

            try {
                kensuke.getClient().send(new PacketEndSession(sessionId));
            } catch (Exception ignored) {
            }

            if (dataRequired) {
                throw new KensukeException(RemoteException.ErrorLevel.SEVERE, "Failed to create session");
            }

            context = new DataContextImpl(kensuke.getGson(), userId.toString(), new HashMap<>());

        }

        System.out.println("Loaded " + event.getName() + " in " + (System.currentTimeMillis() - session.getCreationTime()) + "ms.");

        for (UserManager<?> manager : loadEvent.getUserManagers()) {
            try {
                createGenericUser(session, manager, context);
            } catch (Exception ex) {
                kensuke.getClient().send(new PacketEndSession(sessionId));
                throw ex;
            }
        }

    }

    private <T extends IKensukeUser> void createGenericUser(Session session, UserManager<T> userManager, DataContext context) {
        T user = userManager.createUser(session, context);
        session.getUserObjects().put(userManager, user);
    }


}
