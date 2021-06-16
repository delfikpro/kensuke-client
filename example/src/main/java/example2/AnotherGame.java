package example2;

import com.google.gson.Gson;
import dev.implario.kensuke.Kensuke;
import dev.implario.kensuke.Scope;
import dev.implario.kensuke.UserManager;
import dev.implario.kensuke.impl.KensukeImpl;
import dev.implario.kensuke.SimpleUserManager;
import implario.LoggerUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;

public class AnotherGame extends JavaPlugin implements Listener {

    public static Scope<AnotherData> scope = new Scope<>("another", AnotherData.class);

    public static UserManager<AnotherUser> userManager = new SimpleUserManager<>(
            Collections.singletonList(scope),
            (session, ctx) -> new AnotherUser(session, ctx.getData(scope)),
            (user, ctx) -> ctx.store(scope, user.getData())
    ).setOptional(true);

    @Override
    public void onEnable() {

        Bukkit.getPluginManager().registerEvents(this, this);

        Kensuke kensuke = new KensukeImpl(LoggerUtils.simpleLogger("Kensuke"), new Gson());

        kensuke.addGlobalUserManager(userManager);

    }


}
