package dev.implario.kensuke.impl.bukkit;

import dev.implario.kensuke.IKensukeUser;
import org.bukkit.entity.Player;

public interface IBukkitKensukeUser extends IKensukeUser {

    Player getPlayer();

    void setPlayer(Player player);

    default boolean isPlayerAvailable() {
        return this.getPlayer() != null;
    }

}
