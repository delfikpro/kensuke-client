package dev.implario.kensuke.impl.packet;

import lombok.Data;

import java.util.UUID;

@Data
public class PacketRequestSync {

	private final UUID session;

}
