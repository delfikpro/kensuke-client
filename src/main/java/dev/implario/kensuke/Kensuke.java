package dev.implario.kensuke;

import com.google.gson.Gson;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface Kensuke {

    Gson getGson();

    void setGson(Gson gson);

    CompletableFuture<DataContext> getData(String dataId, Scope<?>... scopes);

    CompletableFuture<?> putData(String dataId, Consumer<DataContext> data);

    CompletableFuture<Void> startSession(KensukeSession session);

    void endSession(KensukeSession session);

    CompletableFuture<Void> saveSession(KensukeSession session);

    KensukeSession getSession(UUID sessionId);

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
