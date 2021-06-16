package dev.implario.kensuke.impl.packet;

import lombok.Data;

import java.util.List;

@Data
public class PacketRequestLeaderboard {

	private final String scope;
	private final String field;
	private final int limit;
	private final List<String> additionalScopes;

}
