package example;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import ru.cristalix.core.CoreApi;
import ru.cristalix.core.IServerPlatform;
import ru.cristalix.core.realm.IRealmService;
import ru.cristalix.core.realm.RealmInfo;
import ru.cristalix.core.stats.PlayerScope;
import ru.cristalix.core.stats.Scope;
import ru.cristalix.core.stats.UserManager;
import ru.cristalix.core.stats.IStatService;
import ru.cristalix.core.stats.impl.StatService;
import ru.cristalix.core.stats.impl.network.StatServiceConnectionData;

public class SomeGame extends JavaPlugin {

	public static final Scope<SomeGameStats> statsScope = new PlayerScope<>("somegame", SomeGameStats.class);

	private UserManager<SomeGameUser> userManager;

	@Override
	public void onEnable() {
		IStatService statService = new StatService(IServerPlatform.get(), StatServiceConnectionData.fromEnvironment());
		CoreApi.get().registerService(IStatService.class, statService);

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
