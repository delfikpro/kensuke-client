package dev.implario.kensuke;

import implario.Environment;
import lombok.Data;

import java.util.Objects;
import java.util.UUID;

@Data
public class KensukeConnectionData {

	private final String host;
	private final int port;
	private final String login;
	private final String password;
	private final String nodeName;

	public static KensukeConnectionData fromEnvironment() {
		return new KensukeConnectionData(
				Environment.require("KENSUKE_HOST"),
				Environment.requireInt("KENSUKE_PORT", 1, 65535),
				Environment.require("KENSUKE_LOGIN"),
				Environment.require("KENSUKE_PASSWORD"),
				Environment.get("KENSUKE_NODENAME")
		);
	}

	public void validate() {
		Objects.requireNonNull(host, "host");
		if (port < 0 || port > 65535) throw new IllegalStateException("Port " + port + " is out of range [1 - 65535]!");
		Objects.requireNonNull(login, "login");
		Objects.requireNonNull(password, "password");
	}

}
