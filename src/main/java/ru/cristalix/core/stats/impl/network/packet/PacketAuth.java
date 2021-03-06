package ru.cristalix.core.stats.impl.network.packet;

import lombok.Data;
import ru.cristalix.core.stats.impl.network.StatServicePacket;

@Data
public
class PacketAuth implements StatServicePacket {

	private final String login;
	private final String password;
	private final String nodeName;

}
