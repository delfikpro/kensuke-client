package dev.implario.kensuke.impl.network;

import dev.implario.kensuke.impl.network.packet.PacketError;
import lombok.Getter;

@Getter
public class KensukeException extends Exception {

	private final PacketError.ErrorLevel errorLevel;

	public KensukeException(PacketError.ErrorLevel errorLevel, String s) {
		super(s);
		this.errorLevel = errorLevel;
	}

	public KensukeException(PacketError.ErrorLevel errorLevel, String s, Throwable throwable) {
		super(s, throwable);
		this.errorLevel = errorLevel;
	}

}
