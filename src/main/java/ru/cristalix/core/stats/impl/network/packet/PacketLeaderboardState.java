package ru.cristalix.core.stats.impl.network.packet;

import com.google.gson.JsonObject;
import lombok.Data;
import ru.cristalix.core.stats.impl.network.StatServicePacket;

@Data
public class PacketLeaderboardState implements StatServicePacket {

	private final JsonObject[] entries;

}
