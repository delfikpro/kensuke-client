package ru.cristalix.core.stats.impl.network;

import implario.Environment;
import lombok.Data;
import ru.cristalix.core.lib.Preconditions;
import ru.cristalix.core.realm.IRealmService;

import java.util.Objects;

@Data
public class StatServiceConnectionData {

	private final String host;
	private final int port;
	private final String login;
	private final String password;
	private final String nodeName;

	public static StatServiceConnectionData fromEnvironment() {
		return new StatServiceConnectionData(
				Environment.require("STAT_SERVICE_HOST"),
				Environment.requireInt("STAT_SERVICE_PORT", 1, 65535),
				Environment.require("STAT_SERVICE_LOGIN"),
				Environment.require("STAT_SERVICE_PASSWORD"),
				Environment.get("STAT_SERVICE_NODENAME", IRealmService.get().getCurrentRealmInfo().getRealmId().getRealmName())
		);
	}

	public void validate() {
		Objects.requireNonNull(host, "host");
		Preconditions.checkState(port > 0 && port <= 65535, "Port " + port + " is out of range [1 - 65535]!");
		Objects.requireNonNull(login, "login");
		Objects.requireNonNull(password, "password");
	}

}
