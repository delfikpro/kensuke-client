package dev.implario.kensuke.impl.bukkit;

import com.google.gson.Gson;
import dev.implario.kensuke.Kensuke;
import dev.implario.kensuke.KensukeConnectionData;
import dev.implario.kensuke.impl.KensukeImpl;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class BukkitKensuke extends JavaPlugin {

    @Getter
    @Setter
    private static Kensuke instance;

    @Getter
    @Setter
    private static Gson gson;

    public static Kensuke setup(Plugin plugin) {
        return setup(plugin, new Gson());
    }

    public static Kensuke setup(Plugin plugin, Gson gson) {
        KensukeImpl kensuke = new KensukeImpl(Logger.getLogger("Kensuke"), gson);
        instance = kensuke;
        new BukkitKensukeAdapter(kensuke, plugin).init();
        kensuke.connect(KensukeConnectionData.fromEnvironment());
        return kensuke;
    }

    @Override
    public void onEnable() {
        setup(this, gson == null ? new Gson() : gson);
    }
}
