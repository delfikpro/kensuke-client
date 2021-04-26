package example;

import lombok.Getter;
import lombok.experimental.Delegate;
import dev.implario.kensuke.DataContext;
import dev.implario.kensuke.player.PlayerWrapper;

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

	public void save(DataContext writer) {
		writer.store(SomeGame.statsScope, stats);
	}

}
