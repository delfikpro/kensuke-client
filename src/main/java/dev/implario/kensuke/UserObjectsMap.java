package dev.implario.kensuke;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.*;

@SuppressWarnings({"rawtypes", "unchecked"})
@RequiredArgsConstructor
public class UserObjectsMap implements Iterable<UserObjectsMap.Entry<?>> {

    private final Map<UserManager<?>, IKensukeUser> underlyingMap;

    public UserObjectsMap() {
        this(new HashMap<>());
    }


    public int size() {
        return underlyingMap.size();
    }

    public boolean isEmpty() {
        return underlyingMap.isEmpty();
    }

    public boolean containsKey(UserManager<? extends IKensukeUser> key) {
        return underlyingMap.containsKey(key);
    }

    public boolean containsValue(IKensukeUser value) {
        return underlyingMap.containsValue(value);
    }

    public <T extends IKensukeUser> T get(UserManager<T> userManager) {
        return (T) underlyingMap.get(userManager);
    }

    public <T extends IKensukeUser> IKensukeUser put(UserManager<T> userManager, T user) {
        return underlyingMap.put(userManager, user);
    }

    public <T extends IKensukeUser> T remove(UserManager<T> userManager) {
        return (T) underlyingMap.remove(userManager);
    }

    public void clear() {
        underlyingMap.clear();
    }

    public Set<UserManager<?>> keySet() {
        return underlyingMap.keySet();
    }

    public Collection<IKensukeUser> values() {
        return underlyingMap.values();
    }

    public Set<Entry<?>> entrySet() {
        Set<Entry<?>> set = new HashSet<>();
        for (Map.Entry<UserManager<?>, IKensukeUser> entry : underlyingMap.entrySet()) {
            set.add(new Entry(entry.getKey(), entry.getValue()));
        }
        return set;
    }

    @Override
    public Iterator<Entry<?>> iterator() {
        return entrySet().iterator();
    }

    @Data
    public static class Entry<T extends IKensukeUser> {

        private final UserManager<T> userManager;
        private final T user;

        public void writeData(DataContext context) {
            userManager.saveUser(user, context);
        }

        public void add() {
            userManager.addUser(user);
        }

        public void remove() {
            userManager.removeUser(user);
        }

    }

}

