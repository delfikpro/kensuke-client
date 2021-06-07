package dev.implario.kensuke.impl;

import com.google.gson.Gson;
import dev.implario.kensuke.*;
import dev.implario.kensuke.impl.packet.*;
import dev.implario.nettier.Nettier;
import dev.implario.nettier.NettierClient;
import dev.implario.nettier.PacketQualifier;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public class KensukeImpl implements Kensuke {

    private final List<UserManager<?>> globalUserManagers = new ArrayList<>();

    @Setter
    private String globalRealm;

    private final Logger logger;
    private final Gson gson;
    private final NettierClient client;
    private KensukeConnectionData connectionData;

    public KensukeImpl(Logger logger, Gson gson) {
        this.logger = logger;
        this.gson = gson;
        this.client = Nettier.createClient(gson, logger);
    }

    public void connect(KensukeConnectionData connectionData) {

        this.connectionData = connectionData;

        client.setPacketTranslator((packet, expected) -> {
            if (packet instanceof PacketError) {
                PacketError error = (PacketError) packet;
                throw new KensukeException(error.getErrorLevel(), error.getMessage());
            }
            return packet;
        });
        client.getQualifier().getQualifiers().add(new PacketQualifier() {
            @Override
            public String getTypeForPacket(Object o) {
                Class<?> clazz = o.getClass();
                if (clazz == PacketOk.class) return "ok";
                if (clazz == PacketError.class) return "error";
                if (clazz == PacketAuth.class) return "auth";
                if (clazz == PacketCreateSession.class) return "createSession";
                if (clazz == PacketEndSession.class) return "endSession";
                if (clazz == PacketSyncData.class) return "syncData";
                if (clazz == PacketRequestSync.class) return "requestSync";
//                if (clazz == PacketUseScopes.class) return "useScopes";
                if (clazz == PacketKeepAlive.class) return "keepAlive";
//                if (clazz == PacketRequestLeaderboard.class) return  "requestLeaderboard";
//                if (clazz == PacketLeaderboardState.class) return  "leaderboardState";
                if (clazz == PacketRequestSnapshot.class) return  "requestSnapshot";
                if (clazz == PacketDataSnapshot.class) return  "dataSnapshot";
                return null;
            }

            @Override
            public Class<?> getClassForType(String s) {
                switch (s) {
                    case "ok": return PacketOk.class;
                    case "error": return PacketError.class;
                    case "auth": return PacketAuth.class;
                    case "createSession": return PacketCreateSession.class;
                    case "endSession": return PacketEndSession.class;
                    case "syncData": return PacketSyncData.class;
                    case "requestSync": return PacketRequestSync.class;
//                    case "useScopes": return PacketUseScopes.class;
                    case "keepAlive": return PacketKeepAlive.class;
//                    case "requestLeaderboard": return PacketRequestLeaderboard.class;
//                    case "leaderboardState": return PacketLeaderboardState.class;
                    case "requestSnapshot": return PacketRequestSnapshot.class;
                    case "dataSnapshot": return PacketDataSnapshot.class;
                    default: return null;
                }
            }
        });
        client.setHandshakeHandler(remote -> {
            remote.send(new PacketAuth(connectionData.getLogin(), connectionData.getPassword(), connectionData.getNodeName()))
                    .await(PacketOk.class, 100, TimeUnit.HOURS);
        });
        client.connect(connectionData.getHost(), connectionData.getPort());


    }

    @Override
    public CompletableFuture<DataContext> getData(String dataId, Scope<?>... scopes) {
        return client.send(new PacketRequestSnapshot(dataId, Arrays.asList(scopes)))
                .awaitFuture(PacketDataSnapshot.class).thenApply(data -> new DataContextImpl(gson, dataId, data.getData()));
    }

    @Override
    public CompletableFuture<DataContext> createSession(UUID sessionId, String dataId, String realm, Scope<?>... scopes) {
        return client.send(new PacketCreateSession(sessionId, dataId, realm, Arrays.stream(scopes).map(Scope::getId).collect(Collectors.toList())))
        .awaitFuture(PacketSyncData.class).thenApply(packet -> new DataContextImpl(gson, dataId, packet.getData()));
    }

    @Override
    public CompletableFuture<?> putData(String dataId, Consumer<DataContext> data) {
        DataContextImpl context = new DataContextImpl(gson, dataId, new HashMap<>());
        data.accept(context);
        return client.send(new PacketDataSnapshot(dataId, context.getDictionary())).awaitFuture(PacketOk.class);
    }

    @Override
    public void addGlobalUserManager(UserManager<?> userManager) {
        this.globalUserManagers.add(userManager);
    }

}