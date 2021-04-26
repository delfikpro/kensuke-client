package dev.implario.kensuke;

import dev.implario.kensuke.scope.Scope;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface UserPool<T> extends DataDeserializer<T>, DataSerializer<T>, SnapshotPool<T> {

	T getUser(UUID uuid);

	default T getUser(CommandSender sender) {
		if (sender instanceof Player) {
			return getUser(((Player) sender).getUniqueId());
		} else {
			throw new UnsupportedOperationException("Unable to get a user instance for " + sender.getClass().getName());
		}
	}

	void addUser(UUID uuid, T user);

	T removeUser(UUID uuid);

	boolean isUserLoaded(UUID uuid);

	Map<UUID, T> getUserCache();

	List<Scope<?>> getScopes();

	default CompletableFuture<T> getUserOrSnapshot(UUID uuid) {
		T user = getUser(uuid);
		if (user == null) {
			return getSnapshot(uuid);
		} else {
			return CompletableFuture.completedFuture(user);
		}
	}

}
