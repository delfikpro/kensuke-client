package dev.implario.kensuke.impl.network.packet;

import dev.implario.kensuke.impl.network.KensukePacket;
import lombok.Data;

@Data
public class PacketOk implements KensukePacket {

	private final String message;

}
