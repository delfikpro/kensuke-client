package dev.implario.kensuke.impl.packet;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class PacketCreateSession {

	private final UUID session;
	private final String id;
	private final String realm;
	private final List<String> scopes;

}
