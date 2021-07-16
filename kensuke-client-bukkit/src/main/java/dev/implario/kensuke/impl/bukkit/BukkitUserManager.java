package dev.implario.kensuke.impl.bukkit;

import dev.implario.kensuke.*;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class BukkitUserManager<T extends IKensukeUser> extends SimpleUserManager<T> {

    public BukkitUserManager(
            Collection<Scope<?>> scopes,
            BiFunction<KensukeSession, DataContext, T> reader,
            BiConsumer<T, DataContext> writer
    ) {
        super(scopes, reader, writer);
    }

    public T getUser(CommandSender sender) {
        if (!(sender instanceof Player))
            throw new IllegalArgumentException("Provided sender is not a player");
        return getUser(((Player) sender).getUniqueId());
    }

    public Collection<T> getOnlineUsers() {
        return getUserMap().values().stream()
                .filter(u -> u instanceof IBukkitKensukeUser && ((IBukkitKensukeUser) u).isPlayerAvailable())
                .collect(Collectors.toList());
    }

}
