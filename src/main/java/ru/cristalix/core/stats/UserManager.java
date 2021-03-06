package ru.cristalix.core.stats;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface UserManager<T> extends UserProvider<T>, UserSerializer<T> {

	void addUser(UUID uuid, T user);

	T removeUser(UUID uuid);

	Map<UUID, T> getUserMap();

	T getUser(UUID uuid);

	default T getUser(Player player) {
		return this.getUser(player.getUniqueId());
	}

	default T getUser(CommandSender sender) {
		if (sender instanceof Player)
			return this.getUser((Player) sender);
		else
			throw new UnsupportedOperationException("Unable to get a user instance for " + sender.getClass().getName());
	}


}
