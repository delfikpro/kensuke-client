package dev.implario.kensuke.impl.packet;

import com.google.gson.JsonElement;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PacketLeaderboardState {

	private final List<LeaderboardEntry> entries;

	@Data
	public static class LeaderboardEntry {
		private final String id;
		private final int position;
		private final Map<String, JsonElement> data;
	}

}
