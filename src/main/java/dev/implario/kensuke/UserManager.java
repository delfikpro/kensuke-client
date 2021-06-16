package dev.implario.kensuke;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface UserManager<U extends IKensukeUser> {

    Collection<Scope<?>> getScopes();

    U getUser(UUID uuid);

    U createUser(Session session, DataContext context);

    void saveUser(U user, DataContext context);

    UserManager<U> setOptional(boolean optional);

    boolean isOptional();

    void addUser(U user);

    boolean removeUser(U user);

}
