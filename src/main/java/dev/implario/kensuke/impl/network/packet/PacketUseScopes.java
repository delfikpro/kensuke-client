package dev.implario.kensuke.impl.network.packet;

import dev.implario.kensuke.impl.network.KensukePacket;
import lombok.Data;

import java.util.List;

@Data
public class PacketUseScopes implements KensukePacket {

	private final List<String> scopes;

}
