package dev.implario.kensuke.impl.network.packet;

import dev.implario.kensuke.impl.network.KensukePacket;
import lombok.Data;

import java.util.UUID;

@Data
public class PacketRequestSync implements KensukePacket {

	private final UUID session;

}
