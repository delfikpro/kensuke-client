package dev.implario.kensuke.impl.network.packet;

import dev.implario.kensuke.impl.network.KensukePacket;
import lombok.Data;

@Data
public class PacketError implements KensukePacket {

	private final ErrorLevel errorLevel;
	private final String errorMessage;

	public enum ErrorLevel {
		FATAL, SEVERE, WARNING, TIMEOUT
	}

}
