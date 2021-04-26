package dev.implario.kensuke.impl.network.packet;

import dev.implario.kensuke.impl.network.KensukePacket;
import lombok.Data;

@Data
public class PacketRequestLeaderboard implements KensukePacket {

	private final String scope;
	private final String field;
	private final int limit;

}
