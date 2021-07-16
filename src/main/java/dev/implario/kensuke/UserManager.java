package dev.implario.kensuke;

import java.util.Collection;
import java.util.UUID;

public interface UserManager<U extends IKensukeUser> {

    Collection<Scope<?>> getScopes();

    U getUser(UUID uuid);

    U createUser(KensukeSession session, DataContext context);

    void saveUser(U user, DataContext context);

    UserManager<U> setOptional(boolean optional);

    boolean isOptional();

    void addUser(U user);

    boolean removeUser(U user);

}
