package ru.cristalix.core.stats.impl.network;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface Dialogue {

	UUID getUuid();

//	String getRealm();

	Dialogue send(StatServicePacket packet);

	<T extends StatServicePacket> CompletableFuture<T> awaitFuture(Class<T> type);

	<T extends StatServicePacket> T await(Class<T> type) throws StatServiceException;

}
