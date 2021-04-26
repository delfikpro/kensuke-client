package dev.implario.kensuke;

import dev.implario.kensuke.scope.Scope;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface SnapshotPool<T> extends DataDeserializer<T> {

    CompletableFuture<T> getSnapshot(UUID uuid);

    default CompletableFuture<T> getSnapshot(CommandSender sender) {
        if (sender instanceof Player) {
            return getSnapshot(((Player) sender).getUniqueId());
        } else {
            throw new UnsupportedOperationException("Unable to get a user instance for " + sender.getClass().getName());
        }
    }

    T getSnapshotNow(UUID uuid);

    default T getSnapshotNow(CommandSender sender) {
        if (sender instanceof Player) {
            return getSnapshotNow(((Player) sender).getUniqueId());
        } else {
            throw new UnsupportedOperationException("Unable to get a user instance for " + sender.getClass().getName());
        }
    }

    void addSnapshot(UUID uuid, T snapshot);

    T removeSnapshot(UUID uuid);

    boolean isSnapshotLoaded(UUID uuid);

    Map<UUID, T> getSnapshotCache();

    List<Scope<?>> getScopes();

}
