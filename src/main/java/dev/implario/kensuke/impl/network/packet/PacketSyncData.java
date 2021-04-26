package dev.implario.kensuke.impl.network.packet;

import com.google.gson.JsonElement;
import dev.implario.kensuke.impl.network.KensukePacket;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class PacketSyncData implements KensukePacket {

	private final UUID session;
	private final Map<String, JsonElement> stats; //TODO: rename to data

}
