package ru.cristalix.core.stats.impl.network.packet;

import lombok.Data;
import ru.cristalix.core.stats.impl.network.StatServicePacket;

@Data
public class PacketOk implements StatServicePacket {

	private final String message;

}
