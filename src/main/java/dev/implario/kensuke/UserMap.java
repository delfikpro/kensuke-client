package dev.implario.kensuke;

import dev.implario.kensuke.impl.DataContextImpl;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.xml.ws.RespectBinding;
import java.util.*;

@SuppressWarnings({"rawtypes", "unchecked"})
@Getter
public class UserMap implements Iterable<UserMap.Entry<?>> {

    private final List<UserMap.Entry<?>> entries = new ArrayList<>();

    public int size() {
        return entries.size();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public <T extends IKensukeUser> T getUser(UserManager<T> userManager) {
        Entry<T> entry = getEntry(userManager);
        return entry == null ? null : entry.getUser();
    }

    public <T extends IKensukeUser> Entry<T> getEntry(UserManager<T> userManager) {
        for (Entry<?> entry : entries) {
            if (entry.getUserManager() == userManager) {
                return (Entry<T>) entry;
            }
        }
        return null;
    }

    public <T extends IKensukeUser> void put(UserManager<T> userManager, T user) {
        Entry<T> entry = getEntry(userManager);
        if (entry != null) entry.setUser(user);
        else entries.add(new Entry<>(userManager));
    }

    @Override
    public Iterator<Entry<?>> iterator() {
        return entries.iterator();
    }

    @Data
    @RequiredArgsConstructor
    @AllArgsConstructor
    public static class Entry<T extends IKensukeUser> {

        private final UserManager<T> userManager;
        private T user;

        public void writeData(DataContext context) {
            userManager.saveUser(user, context);
        }

        public void add() {
            userManager.addUser(user);
        }

        public void remove() {
            if (user != null) userManager.removeUser(user);
        }

        public void initUser(Session session, DataContext context) {
            this.user = userManager.createUser(session, context);
            this.add();
        }
    }

}

