package dev.implario.kensuke.impl;

import com.google.gson.Gson;
import dev.implario.kensuke.*;
import dev.implario.kensuke.impl.packet.*;
import dev.implario.nettier.*;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Getter
public class KensukeImpl implements Kensuke {

    private final List<UserManager<?>> globalUserManagers = new ArrayList<>();

    @Setter
    private String globalRealm;

    private final Logger logger;

    @Getter
    @Setter
    private Gson gson;

    private final NettierClient client;
    private KensukeConnectionData connectionData;

    private final Map<UUID, KensukeSession> sessionMap = new ConcurrentHashMap<>();

    private final Set<PacketSyncData> pendingSaves = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public KensukeImpl(Logger logger, Gson gson) {
        this.logger = logger;
        this.gson = gson;
        this.client = Nettier.createClient(gson, logger);
    }

    public void connect(KensukeConnectionData connectionData) {

        this.connectionData = connectionData;

//        ((NettierClientImpl) client).setDebugReads(true);
//        ((NettierClientImpl) client).setDebugWrites(true);

        client.setPacketTranslator((packet, expected) -> {
            if (packet instanceof PacketError) {
                PacketError error = (PacketError) packet;
                throw new KensukeException(error.getErrorLevel(), error.getErrorMessage());
            }
            return packet;
        });
        client.getQualifier().getQualifiers().add(0, new PacketQualifier() {
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
                if (clazz == PacketRequestLeaderboard.class) return "requestLeaderboard";
                if (clazz == PacketLeaderboardState.class) return "leaderboardState";
                if (clazz == PacketRequestSnapshot.class) return "requestSnapshot";
                if (clazz == PacketDataSnapshot.class) return "dataSnapshot";
                return null;
            }

            @Override
            public Class<?> getClassForType(String s) {
                switch (s) {
                    case "ok":
                        return PacketOk.class;
                    case "error":
                        return PacketError.class;
                    case "auth":
                        return PacketAuth.class;
                    case "createSession":
                        return PacketCreateSession.class;
                    case "endSession":
                        return PacketEndSession.class;
                    case "syncData":
                        return PacketSyncData.class;
                    case "requestSync":
                        return PacketRequestSync.class;
//                    case "useScopes": return PacketUseScopes.class;
                    case "keepAlive":
                        return PacketKeepAlive.class;
                    case "requestLeaderboard":
                        return PacketRequestLeaderboard.class;
                    case "leaderboardState":
                        return PacketLeaderboardState.class;
                    case "requestSnapshot":
                        return PacketRequestSnapshot.class;
                    case "dataSnapshot":
                        return PacketDataSnapshot.class;
                    default:
                        return null;
                }
            }
        });
        client.setHandshakeHandler(remote -> {
            try {
                remote.send(new PacketAuth(
                        connectionData.getLogin(),
                        connectionData.getPassword(),
                        connectionData.getNodeName(),
                        new ArrayList<>(sessionMap.keySet())
                )).await(PacketOk.class, 2, TimeUnit.SECONDS);
            } catch (Exception ex) {
                ex.printStackTrace();
                client.close();
            }
        });

        client.addListener(PacketRequestSync.class, (talk, request) -> {

            UUID sessionId = request.getSession();
            KensukeSession session = sessionMap.get(sessionId);
            if (session == null) {
                talk.respond(new PacketError(RemoteException.ErrorLevel.SEVERE, "No such session"));
                return;
            }

            if (session.getState() == SessionState.UNAPPROVED) {
                talk.respond(new PacketError(RemoteException.ErrorLevel.WARNING, "Nothing to sync yet"));
                return;
            }

            if (session.getState() == SessionState.DEAD) {
                talk.respond(new PacketError(RemoteException.ErrorLevel.WARNING, "Session already dead"));
                return;
            }

            DataContext context;
            try {
                context = createSave(session);
            } catch (Exception exception) {
                logger.log(Level.SEVERE, "Error while creating a save for " + session.getUserId(), exception);
                talk.respond(new PacketError(RemoteException.ErrorLevel.SEVERE, "Sync failed"));
                return;
            }

            talk.respond(new PacketSyncData(sessionId, context.getDictionary()));
        });

        client.addListener(PacketEndSession.class, (talk, request) -> {

            KensukeSession session = sessionMap.get(request.getSession());

            if (session == null) {
                logger.warning("Service asked us to end " + request.getSession() + " session, but we don't have it!");
                return;
            }

            endSession(session);

        });

        client.connect(connectionData.getHost(), connectionData.getPort());


    }

    @Override
    public CompletableFuture<Void> saveSession(KensukeSession session) {
        DataContext save;
        try {
            save = createSave(session);
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Error while creating a save for " + session.getUserId(), exception);
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(exception);
            return future;
        }

        PacketSyncData packet = new PacketSyncData(session.getSessionId(), save.getDictionary());

        return client.send(packet)
                .awaitFuture(PacketOk.class).handle((ok, error) -> {
                    if (error != null) {
                        logger.log(Level.SEVERE, "Error while saving " + session.getUserId(), error);
                        throw new KensukeException(RemoteException.ErrorLevel.SEVERE, error.getMessage(), error);
                    }
                    return null;
                });

    }

    protected DataContext createSave(KensukeSession session) {
        if (!session.isActive())
            throw new KensukeException(RemoteException.ErrorLevel.SEVERE, "Tried to save inactive session " + session);
        DataContextImpl dataContext = new DataContextImpl(gson, session.getUserId(), new HashMap<>());
        session.createSave(dataContext);
        return dataContext;
    }

    @Override
    public CompletableFuture<DataContext> getData(String dataId, Scope<?>... scopes) {
        return client.send(new PacketRequestSnapshot(dataId, Arrays.asList(scopes)))
                .awaitFuture(PacketDataSnapshot.class).thenApply(data -> new DataContextImpl(gson, dataId, data.getData()));
    }

    @Override
    public CompletableFuture<Void> startSession(KensukeSession session) {

        boolean isDataRequired = session.getUserObjects().getEntries().stream()
                .anyMatch(entry -> !entry.getUserManager().isOptional());

        List<String> scopes = session.getUserObjects().getEntries().stream()
                .flatMap(entry -> entry.getUserManager().getScopes().stream())
                .map(Scope::getId)
                .collect(Collectors.toList());

        PacketCreateSession packet = new PacketCreateSession(session.getSessionId(), session.getUserId(), scopes);

        return client.send(packet)
                .awaitFuture(PacketSyncData.class)
                .handle((data, error) -> {

                    if (error != null) {
                        logger.log(Level.SEVERE, "Error while fetching data for " + session.getUserId(), error);

                        endSession(session);

                        if (isDataRequired) {
                            throw new KensukeException(RemoteException.ErrorLevel.SEVERE, "Failed to create session: " + error.getMessage());
                        }
                    }

                    DataContextImpl context = new DataContextImpl(gson, session.getUserId(),
                            data == null ? new HashMap<>() : data.getData());

                    for (UserMap.Entry<?> entry : session.getUserObjects()) {
                        try {
                            entry.initUser(session, context);
                        } catch (Exception ex) {
                            endSession(session);
                            throw ex;
                        }
                    }

                    sessionMap.put(session.getSessionId(), session);

                    if (error == null) {
                        session.setState(SessionState.UNAPPROVED);
                    } else {
                        session.setState(SessionState.DEAD);
                    }

                    return null;

                });
    }

    @Override
    public KensukeSession getSession(UUID sessionId) {
        return sessionMap.get(sessionId);
    }

    @Override
    public void endSession(KensukeSession session) {
        session.setState(SessionState.DEAD);
        sessionMap.remove(session.getSessionId());
        for (UserMap.Entry<?> entry : session.getUserObjects()) {
            entry.remove();
        }
        try {
            client.send(new PacketEndSession(session.getSessionId()));
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Couldn't end session " + session.getSessionId() + " for " + session.getUserId(), ex);
        }
    }

    @Override
    public CompletableFuture<List<LeaderboardEntry<DataContext>>> getLeaderboard(
            Scope<?> criterionScope,
            String criterion,
            int limit,
            Scope<?>... additionalScopes
    ) {
        PacketRequestLeaderboard packet = new PacketRequestLeaderboard(criterionScope.getId(), criterion, limit,
                Arrays.stream(additionalScopes).map(Scope::getId).collect(Collectors.toList()));

        return client.send(packet)
                .awaitFuture(PacketLeaderboardState.class).thenApply(p -> p.getEntries().stream()
                        .map(e -> new LeaderboardEntry<DataContext>(e.getPosition(),
                                new DataContextImpl(gson, e.getId(), e.getData())))
                        .collect(Collectors.toList()));
    }

    @Override
    public <U extends IKensukeUser> CompletableFuture<List<LeaderboardEntry<U>>> getLeaderboard(
            UserManager<U> manager,
            Scope<?> criterionScope,
            String criterionField,
            int limit
    ) {
        return getLeaderboard(criterionScope, criterionField, limit, manager.getScopes().toArray(new Scope[0]))
                .thenApply(list -> list.stream()
                        .map(entry -> {
                            KensukeSession session = new KensukeSession(null, entry.getData().getId());
                            return new LeaderboardEntry<>(entry.getPosition(), manager.createUser(session, entry.getData()));
                        }).collect(Collectors.toList()));
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