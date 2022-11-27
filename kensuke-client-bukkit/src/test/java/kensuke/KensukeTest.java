package kensuke;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import dev.implario.kensuke.*;
import dev.implario.kensuke.impl.KensukeImpl;
import dev.implario.kensuke.impl.bukkit.BukkitKensukeAdapter;
import dev.implario.kensuke.impl.packet.*;
import dev.implario.nettier.Nettier;
import dev.implario.nettier.NettierServer;
import dev.implario.nettier.RemoteException;
import dev.implario.nettier.impl.client.NettierClientImpl;
import dev.implario.nettier.impl.server.NettierServerImpl;
import implario.LoggerUtils;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("rawtypes")
public class KensukeTest {

    private static final Gson gson = new Gson();
    private static Plugin plugin;

    private static final Map<Class, Consumer> eventHandlers = new HashMap<>();
    private static KensukeImpl kensuke;

    @Data
    public static class TestStats {
        int i = 0;
        int j = 10100;
    }

    private static final Scope<TestStats> testScope = new Scope<>("testscope", TestStats.class);

    @Test
    @Order(1)
    public void prepareBukkit() {

        System.out.println("Preparing bukkit...");

        Logger bukkitLogger = LoggerUtils.simpleLogger("Bukkit");

        plugin = mock(Plugin.class);
        Server bukkitServer = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);

        doReturn(bukkitLogger).when(bukkitServer).getLogger();
        doReturn(scheduler).when(bukkitServer).getScheduler();

        doReturn(true).when(plugin).isEnabled();

        PluginManager pluginManager = mock(PluginManager.class);

        doAnswer(invocation -> {
            BukkitKensukeAdapter adapter = invocation.getArgument(0);

            eventHandlers.put(PlayerQuitEvent.class, (Consumer<PlayerQuitEvent>) adapter::onQuit);
            eventHandlers.put(AsyncPlayerPreLoginEvent.class, (Consumer<AsyncPlayerPreLoginEvent>) adapter::onPreLogin);
            eventHandlers.put(PlayerLoginEvent.class, (Consumer<PlayerLoginEvent>) adapter::onLogin);
            eventHandlers.put(PlayerJoinEvent.class, (Consumer<PlayerJoinEvent>) adapter::onJoin);

            return null;
        }).when(pluginManager).registerEvents(any(), eq(plugin));

        doReturn(pluginManager).when(bukkitServer).getPluginManager();

        Bukkit.setServer(bukkitServer);

    }

    @SuppressWarnings("unchecked")
    public static void fireEvent(Event event) {
        eventHandlers.get(event.getClass()).accept(event);
    }

    @Test
    @Order(2)
    public void prepareKensuke() throws Exception {

        System.out.println("Preparing kensuke...");
        NettierServer server = Nettier.createServer(gson, LoggerUtils.simpleLogger("KensukeServer"));

        kensuke = new KensukeImpl(LoggerUtils.simpleLogger("KensukeClient"), new Gson());

        ((NettierServerImpl) server).setDebugReads(true);
        ((NettierServerImpl) server).setDebugWrites(true);
        ((NettierClientImpl) kensuke.getClient()).setDebugReads(true);
        ((NettierClientImpl) kensuke.getClient()).setDebugWrites(true);

        kensuke.setGlobalRealm("TEST-1");


        server.getQualifier().getQualifiers().add(kensuke.getClient().getQualifier().getQualifiers().get(0));
//        final PacketQualifier debugQualifier = new PacketQualifier() {
//            @Override
//            public String getTypeForPacket(Object o) {
//                System.out.println("Getting type for object " + o.getClass().getName());
//                return null;
//            }
//
//            @Override
//            public Class<?> getClassForType(String s) {
//                System.out.println("Getting type for tag " + s);
//                return null;
//            }
//        };
//
//        kensuke.getClient().getQualifier().getQualifiers().add(0, debugQualifier);
//        server.getQualifier().getQualifiers().add(0, debugQualifier);


        server.addListener(PacketAuth.class, (t, p) -> {
            System.out.println(p);
            t.respond(new PacketOk("Authorized as " + p.getLogin()));
        });


        server.start(49150).await();

        kensuke.connect(new KensukeConnectionData(
                "127.0.0.1", 49150, "test", "test", "testnode"
        ));

        kensuke.getClient().waitUntilReady();

        server.addListener(PacketCreateSession.class, (t, p) -> {

            for (String scope : p.getScopes()) {
                System.out.println("scope " + scope);
                if (!scope.equals(testScope.getId())) {
                    System.out.println("BAD SCOPE!");
                    t.respond(new PacketError(RemoteException.ErrorLevel.FATAL,
                            "You don't have permission to access '" + scope + "' scope."
                    ));
                    return;
                }
            }

            HashMap<String, JsonElement> map = new HashMap<>();
            map.put(testScope.getId(), gson.toJsonTree(new TestStats()));
            t.respond(new PacketSyncData(p.getSession(), map));
        });

        new BukkitKensukeAdapter(kensuke, plugin).init();

    }

    private static final String testPlayerName = "testPlayer";
    private static final UUID testPlayerId = UUID.randomUUID();
    private static final InetAddress testPlayerAddress = InetAddress.getLoopbackAddress();

    @Getter
    @RequiredArgsConstructor
    static class TestUser implements IKensukeUser {

        @Delegate
        private final TestStats data;

        private final KensukeSession session;

    }

    @Test
    @Order(3)
    public void testSimpleJoin() {
       /* SimpleUserManager<TestUser> userManager = new SimpleUserManager<>(Collections.singletonList(testScope),
                (session, context) -> new TestUser(context.getData(testScope), session),
                (user, context) -> context.store(testScope, user.getData())
        );
        kensuke.addGlobalUserManager(userManager);

        AsyncPlayerPreLoginEvent preLoginEvent = new AsyncPlayerPreLoginEvent(testPlayerName, testPlayerAddress, testPlayerId);
        fireEvent(preLoginEvent);

        assertEquals(AsyncPlayerPreLoginEvent.Result.ALLOWED, preLoginEvent.getLoginResult(), preLoginEvent.getKickMessage());

        Player player = mock(Player.class);

        doReturn(testPlayerId).when(player).getUniqueId();

        PlayerLoginEvent loginEvent = new PlayerLoginEvent(player, "127.0.0.1", testPlayerAddress);
        fireEvent(loginEvent);
        assertEquals(PlayerLoginEvent.Result.ALLOWED, loginEvent.getResult(), loginEvent.getKickMessage());

        fireEvent(new PlayerJoinEvent(player, ""));

        assertEquals(1, userManager.getUserMap().size());

        fireEvent(new PlayerQuitEvent(player, ""));

        assertEquals(0, userManager.getUserMap().size());*/

    }

    @Test
    @Order(4)
    public void testSessionCreateError() {
/*

        AsyncPlayerPreLoginEvent e = new AsyncPlayerPreLoginEvent(testPlayerName, testPlayerAddress, testPlayerId);

        kensuke.getGlobalUserManagers().get(0).getScopes().add(new Scope<>("evilScope", Void.class));

        fireEvent(e);

        System.out.println(e.getKickMessage());
        assertEquals(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, e.getLoginResult());
*/

    }

}
