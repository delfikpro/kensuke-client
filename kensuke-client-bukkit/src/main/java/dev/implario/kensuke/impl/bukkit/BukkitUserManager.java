package dev.implario.kensuke.impl.bukkit;

import dev.implario.kensuke.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class BukkitUserManager<T extends IKensukeUser> extends SimpleUserManager<T> {

    public BukkitUserManager(
            Collection<Scope<?>> scopes,
            BiFunction<Session, DataContext, T> reader,
            BiConsumer<T, DataContext> writer
    ) {
        super(scopes, reader, writer);
    }

    public T getUser(CommandSender sender) {
        return sender instanceof Entity ? getUser(((Entity) sender).getUniqueId()) : null;
    }

    public Collection<T> getOnlineUsers() {
        return getUserMap().values().stream()
                .filter(u -> u instanceof IBukkitKensukeUser && ((IBukkitKensukeUser) u).isPlayerAvailable())
                .collect(Collectors.toList());
    }

}
