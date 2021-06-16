package dev.implario.kensuke.impl.bukkit;

import com.google.gson.Gson;
import dev.implario.kensuke.Kensuke;
import dev.implario.kensuke.KensukeConnectionData;
import dev.implario.kensuke.impl.KensukeImpl;
import lombok.experimental.UtilityClass;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

@UtilityClass
public class BukkitKensuke {

    public static Kensuke setup(Plugin plugin) {
        return setup(plugin, new Gson());
    }

    public static Kensuke setup(Plugin plugin, Gson gson) {
        KensukeImpl kensuke = new KensukeImpl(Logger.getLogger("Kensuke"), gson);
        new BukkitKensukeAdapter(kensuke, plugin).init();
        kensuke.connect(KensukeConnectionData.fromEnvironment());
        return kensuke;
    }

}
