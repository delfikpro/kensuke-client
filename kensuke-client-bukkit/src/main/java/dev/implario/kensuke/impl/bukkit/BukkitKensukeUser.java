package dev.implario.kensuke.impl.bukkit;

import dev.implario.kensuke.IKensukeUser;
import dev.implario.kensuke.KensukeUser;
import dev.implario.kensuke.Session;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.bukkit.entity.Player;

@Getter
@Setter
public class BukkitKensukeUser extends KensukeUser implements IBukkitKensukeUser, IKensukeUser {

    private Player player;

    public BukkitKensukeUser(Session session) {
        super(session);
    }
}
