package dev.implario.kensuke;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface Kensuke {

    CompletableFuture<DataContext> getData(String dataId, Scope<?>... scopes);

    CompletableFuture<?> putData(String dataId, Consumer<DataContext> data);

    CompletableFuture<DataContext> createSession(UUID sessionId, String dataId, String realm, Scope<?>... scopes);

    void addGlobalUserManager(UserManager<?> userManager);

    void setGlobalRealm(String globalRealm);

    String getGlobalRealm();
}
