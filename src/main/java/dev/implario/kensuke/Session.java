package dev.implario.kensuke;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.*;

@Data
public class Session {

    private final UUID sessionId;

    private final UUID userId;

    private final long creationTime = System.currentTimeMillis();

    private long lastSave = System.currentTimeMillis();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private final UserObjectsMap userObjects = new UserObjectsMap();

    public <T extends IKensukeUser> T getUser(UserManager<T> userManager) {
        return userObjects.get(userManager);
    }

    public void createSave(DataContext context) {
        for (UserObjectsMap.Entry<? extends IKensukeUser> entry : userObjects) {
            UserManager<? extends IKensukeUser> userManager = entry.getUserManager();
        }
    }

}
