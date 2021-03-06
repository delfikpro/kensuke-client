package ru.cristalix.core.stats.impl.network;

import lombok.Data;
import ru.cristalix.core.stats.impl.network.StatServicePacket;

import java.util.UUID;

@Data
public class StatServicePacketFrame {

	private final String type;
	private final StatServicePacket data;
	private final UUID uuid;

}
