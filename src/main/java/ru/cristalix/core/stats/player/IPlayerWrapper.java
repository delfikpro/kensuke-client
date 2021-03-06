package ru.cristalix.core.stats.player;

import org.bukkit.entity.Player;

public interface IPlayerWrapper {

	Player getPlayer();

	void setPlayer(Player player);

	default boolean isPlayerAvailable() {
		return this.getPlayer() != null;
	}

}
