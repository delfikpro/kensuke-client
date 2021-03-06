package ru.cristalix.core.stats.impl.network.packet;

import lombok.Data;
import ru.cristalix.core.stats.impl.network.StatServicePacket;

import java.util.UUID;

@Data
public class PacketCreateSession implements StatServicePacket {

	private final UUID playerId;
	private final UUID session;
	private final String username;
	private final String realm;

}
