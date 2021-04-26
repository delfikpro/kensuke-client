package dev.implario.kensuke.impl.network.packet;

import dev.implario.kensuke.impl.network.KensukePacket;
import lombok.Data;

import java.util.UUID;

@Data
public class PacketEndSession implements KensukePacket {

	private final UUID session;

}
