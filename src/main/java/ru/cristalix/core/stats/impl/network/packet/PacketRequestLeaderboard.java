package ru.cristalix.core.stats.impl.network.packet;

import lombok.Data;
import ru.cristalix.core.stats.impl.network.StatServicePacket;

@Data
public class PacketRequestLeaderboard implements StatServicePacket {

	private final String scope;
	private final String field;
	private final int limit;

}
