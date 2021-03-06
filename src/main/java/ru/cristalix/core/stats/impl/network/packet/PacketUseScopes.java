package ru.cristalix.core.stats.impl.network.packet;

import lombok.Data;
import ru.cristalix.core.stats.impl.network.StatServicePacket;

import java.util.List;

@Data
public class PacketUseScopes implements StatServicePacket {

	private final List<String> scopes;

}
