package ru.cristalix.core.stats.impl.network;

import lombok.Getter;
import ru.cristalix.core.stats.impl.network.packet.PacketError;

@Getter
public class StatServiceException extends Exception {

	private final PacketError.ErrorLevel errorLevel;

	public StatServiceException(PacketError.ErrorLevel errorLevel, String s) {
		super(s);
		this.errorLevel = errorLevel;
	}

	public StatServiceException(PacketError.ErrorLevel errorLevel, String s, Throwable throwable) {
		super(s, throwable);
		this.errorLevel = errorLevel;
	}

}
