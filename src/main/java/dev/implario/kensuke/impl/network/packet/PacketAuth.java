package dev.implario.kensuke.impl.network.packet;

import dev.implario.kensuke.impl.network.KensukePacket;
import lombok.Data;

@Data
public
class PacketAuth implements KensukePacket {

	private final String login;
	private final String password;
	private final String nodeName;

}
