package dev.implario.kensuke;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.*;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class KensukeSession {

    @EqualsAndHashCode.Include
    private final UUID sessionId;

    private final String userId;

    private final long creationTime = System.currentTimeMillis();

    private long lastSave = System.currentTimeMillis();

    private boolean active = false;

    @ToString.Exclude
    protected final UserMap userObjects = new UserMap();

    public void addUserManager(UserManager<?> userManager) {
        if (userObjects.getEntry(userManager) == null)
            userObjects.put(userManager, null);
    }

    public <T extends IKensukeUser> T getUser(UserManager<T> userManager) {
        return userObjects.getUser(userManager);
    }

    public void createSave(DataContext context) {
        for (UserMap.Entry<? extends IKensukeUser> entry : userObjects) {
            entry.writeData(context);
        }
    }

}
