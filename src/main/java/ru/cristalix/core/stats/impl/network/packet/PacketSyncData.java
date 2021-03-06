package ru.cristalix.core.stats.impl.network.packet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Data;
import ru.cristalix.core.stats.impl.network.StatServicePacket;

import java.util.Map;
import java.util.UUID;

@Data
public class PacketSyncData implements StatServicePacket {

	private final UUID session;
	private final Map<String, JsonElement> stats;

}
