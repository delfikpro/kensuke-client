package ru.cristalix.core.stats.impl.network.packet;

import lombok.Data;
import ru.cristalix.core.stats.impl.network.StatServicePacket;

@Data
public class PacketError implements StatServicePacket {

	private final ErrorLevel errorLevel;
	private final String errorMessage;

	public enum ErrorLevel {
		FATAL, SEVERE, WARNING, TIMEOUT
	}

}
