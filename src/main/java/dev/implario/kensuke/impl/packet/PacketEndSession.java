package dev.implario.kensuke.impl.packet;

import lombok.Data;

import java.util.UUID;

@Data
public class PacketEndSession {

	private final UUID session;

}
