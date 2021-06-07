package dev.implario.kensuke;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = false)
public class UserLoadEvent extends Event {

    @Getter
    private static final HandlerList handlerList = new HandlerList();

    private final AsyncPlayerPreLoginEvent preLoginEvent;

    private final Set<UserManager<?>> userManagers = new HashSet<>();

    private String realm;

    public UserLoadEvent(AsyncPlayerPreLoginEvent preLoginEvent, String realm) {
        super(true);
        this.preLoginEvent = preLoginEvent;
        this.realm = realm;
    }

    public void addUserManager(UserManager<?> userManager) {
        userManagers.add(userManager);
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

}
