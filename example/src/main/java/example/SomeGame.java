package example;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import ru.cristalix.core.CoreApi;
import ru.cristalix.core.IServerPlatform;
import dev.implario.kensuke.scope.PlayerScope;
import dev.implario.kensuke.scope.Scope;
import dev.implario.kensuke.UserPool;
import dev.implario.kensuke.IKensuke;
import dev.implario.kensuke.impl.Kensuke;
import dev.implario.kensuke.impl.network.KensukeConnectionData;

public class SomeGame extends JavaPlugin {

	public static final Scope<SomeGameStats> statsScope = new PlayerScope<>("somegame", SomeGameStats.class);

	private UserPool<SomeGameUser> userManager;

	@Override
	public void onEnable() {
		IKensuke statService = new Kensuke(IServerPlatform.get(), KensukeConnectionData.fromEnvironment());
		CoreApi.get().registerService(IKensuke.class, statService);

		statService.useScopes(statsScope);
		statService.setDataRequired(false);

		this.userManager = statService.registerUserManager(
				reader -> new SomeGameUser(reader.getUuid(), reader.getName(), reader.getData(statsScope)),
				SomeGameUser::save);

		Bukkit.getPluginCommand("stats").setExecutor(this);

	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		SomeGameUser user = userManager.getUser(sender);
		sender.sendMessage("§eРейтинг: §f" + user.getRating());
		return true;

	}

}
