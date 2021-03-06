package ru.cristalix.core.stats.player;

import lombok.*;
import org.bukkit.entity.Player;

import java.util.UUID;

@Data
public class PlayerWrapper implements IPlayerWrapper {

	protected final UUID uuid;

	protected final String name;

	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	protected Player player;

	@Override
	public boolean isPlayerAvailable() {
		return player != null;
	}

	public Player getPlayer() {
		if (!this.isPlayerAvailable())
			throw new IllegalStateException("User " + this + " is not yet loaded as a bukkit player");
		return player;
	}

}
