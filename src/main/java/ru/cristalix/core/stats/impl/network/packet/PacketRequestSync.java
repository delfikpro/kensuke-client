package ru.cristalix.core.stats.impl.network.packet;

import lombok.Data;
import ru.cristalix.core.stats.impl.network.StatServicePacket;

import java.util.UUID;

@Data
public class PacketRequestSync implements StatServicePacket {

	private final UUID session;

}
