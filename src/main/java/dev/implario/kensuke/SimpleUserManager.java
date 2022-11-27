package dev.implario.kensuke;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

@Accessors(chain = true)
public class SimpleUserManager<U extends IKensukeUser> implements UserManager<U> {

    @Getter
    private final Set<Scope<?>> scopes;

    @Getter
    private final Map<UUID, U> userMap = new ConcurrentHashMap<>();

    private final BiFunction<KensukeSession, DataContext, U> reader;

    private final BiConsumer<U, DataContext> writer;

    @Getter
    @Setter
    private boolean optional;

    public SimpleUserManager(Collection<Scope<?>> scopes,
                             BiFunction<KensukeSession, DataContext, U> reader,
                             BiConsumer<U, DataContext> writer) {
        this.scopes = new HashSet<>(scopes);
        this.reader = reader;
        this.writer = writer;
    }

    @Override
    public U createUser(KensukeSession session, DataContext context) {
        return reader.apply(session, context);
    }

    @Override
    public void saveUser(U user, DataContext context) {
        writer.accept(user, context);
    }

    @Override
    public U getUser(UUID uuid) {
        return userMap.get(uuid);
    }

    @Override
    public boolean removeUser(U user) {
        return userMap.remove(user.getSession().getUserId(), user);
    }

    @Override
    public void addUser(U user) {
        userMap.put(user.getSession().getUserId(), user);
    }
}
