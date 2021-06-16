package example2;

import dev.implario.kensuke.Session;
import dev.implario.kensuke.impl.bukkit.BukkitKensukeUser;
import lombok.Getter;
import lombok.experimental.Delegate;

public class AnotherUser extends BukkitKensukeUser {

    @Getter
    @Delegate
    private final AnotherData data;

    public AnotherUser(Session session, AnotherData data) {
        super(session);
        this.data = data;
    }
}
