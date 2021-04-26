package dev.implario.kensuke.impl.network.packet;

import com.google.gson.JsonObject;
import dev.implario.kensuke.impl.network.KensukePacket;
import lombok.Data;

@Data
public class PacketLeaderboardState implements KensukePacket {

	private final JsonObject[] entries;

}
