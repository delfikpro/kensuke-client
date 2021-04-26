package dev.implario.kensuke.impl.network;

import lombok.Data;

import java.util.UUID;

@Data
public class KensukePacketFrame {

	private final String type;
	private final KensukePacket data;
	private final UUID uuid;

}
