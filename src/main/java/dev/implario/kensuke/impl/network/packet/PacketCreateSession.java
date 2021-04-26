package dev.implario.kensuke.impl.network.packet;

import dev.implario.kensuke.impl.network.KensukePacket;
import lombok.Data;

import java.util.UUID;

@Data
public class PacketCreateSession implements KensukePacket {

	private final UUID playerId;
	private final UUID session;
	private final String username;
	private final String realm;

}
