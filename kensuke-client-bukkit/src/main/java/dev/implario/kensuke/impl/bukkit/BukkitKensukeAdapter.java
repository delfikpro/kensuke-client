package dev.implario.kensuke.impl.bukkit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.implario.kensuke.*;
import dev.implario.kensuke.impl.KensukeException;
import dev.implario.kensuke.impl.KensukeImpl;
import dev.implario.nettier.RemoteException;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result.KICK_BANNED;

@RequiredArgsConstructor
public class BukkitKensukeAdapter implements Listener {

    private final KensukeImpl kensuke;
    private final Plugin plugin;

    // Due to lack of better option to distinguish players in pre-login phase, we are forced to use this hack.
    private final Cache<IdentityUUID, KensukeSession> loadingPlayers = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .expireAfterWrite(15, TimeUnit.SECONDS)
            .build();

    private final Map<KensukeSession, Player> sessionToPlayerMap = new HashMap<>();
    private final Map<Player, KensukeSession> playerToSessionMap = new HashMap<>();

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
            for (Map.Entry<KensukeSession, Player> entry : sessionToPlayerMap.entrySet()) {
                KensukeSession session = entry.getKey();
                if (!session.isActive()) continue;
                if (session.getLastSave() + 60000 > time) continue;

                kensuke.saveSession(session);

                session.setLastSave(time);
            }
        }, 1, 1);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {

        try {
            handlePreLogin(event);
        } catch (Throwable exception) {
            exception.printStackTrace();
            event.disallow(KICK_BANNED, exception.getMessage());
        }

    }

    private static final Pattern ownErrorPattern = Pattern.compile("Node ([^ ]+) .+ (\\d+) ms.");

    private void handlePreLogin(AsyncPlayerPreLoginEvent event) throws Throwable {

        // Do not load data if login is already forbidden
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
//            System.out.println("disallowed");
            return;
        }

        UUID userId = event.getUniqueId();
        UUID sessionId = UUID.randomUUID();

        KensukeSession session = new KensukeSession(sessionId, userId.toString());

        UserLoadEvent loadEvent = new UserLoadEvent(event, kensuke.getGlobalRealm());

        loadEvent.getUserManagers().addAll(kensuke.getGlobalUserManagers());

        Bukkit.getPluginManager().callEvent(loadEvent);

        boolean dataUseful = false;

        for (UserManager<?> userManager : loadEvent.getUserManagers()) {
            session.addUserManager(userManager);
            if (!userManager.getScopes().isEmpty())
                dataUseful = true;
        }

//        System.out.println("больше дебага богу дебага");

        // If nobody is using kensuke, then no session will be created.
        // ToDo: Subsequent events are handling this case as an error
        if (!dataUseful) return;

        String realm = loadEvent.getRealm();

        // Disallow logins that do lock some scopes, but didn't receive a realm
        if (realm == null) {
            throw new IllegalStateException("No global realm defined and no one bothered to claim you");
        }

        DataContext context;

        try {
            kensuke.startSession(session).get(2, TimeUnit.SECONDS);

            loadingPlayers.put(new IdentityUUID(event.getUniqueId()), session);
//            System.out.println(loadingPlayers.size() + " players cached pre-login");
        } catch (ExecutionException | TimeoutException | InterruptedException exception) {
            Throwable cause = exception.getCause() != null ? exception.getCause() : exception;
            if (cause instanceof KensukeException) {
                String message = cause.getMessage();
                Matcher matcher = ownErrorPattern.matcher(message);
                if (matcher.find()) {
                    int ms = Integer.parseInt(matcher.group(2));
                    throw new KensukeException(RemoteException.ErrorLevel.SEVERE, "§cСервер " + matcher.group(1) +
                            " оффлайн, а на нём твои несохранённые данные. Сообщи §cразработчикам. Через " + (ms / 1000) +
                            " сек. защита от §cвхода спадёт.");
                }
            }
            throw cause;
        }

        System.out.println("Loaded " + event.getName() + " in " + (System.currentTimeMillis() - session.getCreationTime()) + "ms.");

    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

//        System.out.println(loadingPlayers.size() + " players in loading cache");
        KensukeSession session = loadingPlayers.getIfPresent(new IdentityUUID(event.getPlayer().getUniqueId()));
        if (session == null) {
            kensuke.getLogger().warning("No session found for " + uuid + " (" + player.getName() + ") during login");
//            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, "Kensuke: Вход на сервер занял слишком много времени");
            return;
        }

        sessionToPlayerMap.put(session, player);
        playerToSessionMap.put(player, session);

        session.setState(SessionState.ACTIVE);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLoginMonitor(PlayerLoginEvent e) {
        if (e.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            KensukeSession session = playerToSessionMap.remove(e.getPlayer());
            if (session != null) sessionToPlayerMap.remove(session);
        }
    }


    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        KensukeSession session = playerToSessionMap.get(player);

        if (!session.isActive()) {
            event.getPlayer().sendMessage("§cНам не удалось прогрузить ваши данные,");
            event.getPlayer().sendMessage("§cТекущая игра не попадёт в статистику.");
            event.getPlayer().sendMessage("§cСообщите разработчикам.");
        }

        for (val entry : session.getUserObjects()) {

            IKensukeUser user = entry.getUser();

            if (user instanceof IBukkitKensukeUser) {
                ((IBukkitKensukeUser) user).setPlayer(player);
            }

        }
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        KensukeSession session = playerToSessionMap.remove(player);

        if (session == null) {

            KensukeSession cachedSession = loadingPlayers.getIfPresent(new IdentityUUID(player.getUniqueId()));
            if (cachedSession != null) {
                kensuke.getLogger().warning("Player " + player.getUniqueId() + " (" + player.getName() +
                        ") was tried to be saved during login");
            } else {
                kensuke.getLogger().warning("Player " + player.getUniqueId() + " (" + player.getName() +
                        ") disconnected without having a session");
            }

            return;
        }

        try {
            kensuke.saveSession(session);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        sessionToPlayerMap.remove(session);
        playerToSessionMap.remove(player);
        kensuke.endSession(session);
    }

}
