package dev.implario.kensuke;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface Kensuke {

    CompletableFuture<DataContext> getData(String dataId, Scope<?>... scopes);

    CompletableFuture<?> putData(String dataId, Consumer<DataContext> data);

    CompletableFuture<Void> startSession(Session session);

    void endSession(Session session);

    CompletableFuture<Void> saveSession(Session session);

    Session getSession(UUID sessionId);

    CompletableFuture<List<LeaderboardEntry<DataContext>>> getLeaderboard(
            Scope<?> criterionScope,
            String criterion,
            int limit,
            Scope<?>... additionalScopes
    );

    <U extends IKensukeUser> CompletableFuture<List<LeaderboardEntry<U>>> getLeaderboard(
            UserManager<U> userManager,
            Scope<?> criterionScope,
            String criterionField,
            int limit
    );

    void addGlobalUserManager(UserManager<?> userManager);

    void setGlobalRealm(String globalRealm);

    String getGlobalRealm();

}
