package example;

import lombok.Getter;
import lombok.experimental.Delegate;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.cristalix.core.stats.StatContext;
import ru.cristalix.core.stats.player.PlayerWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

@Getter
public class SomeGameUser extends PlayerWrapper {

	@Delegate
	private final SomeGameStats stats;

	public SomeGameUser(UUID uuid, String name, SomeGameStats stats) {
		super(uuid, name);
		if (stats == null) {
			// Стандартная статистика
			stats = new SomeGameStats(0, 0, 500, Collections.singletonList(new InventoryStat("STONE", 1)));
		}
		this.stats = stats;
	}

	public void save(StatContext writer) {
		writer.store(SomeGame.statsScope, stats);
	}

}
