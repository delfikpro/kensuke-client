package dev.implario.kensuke.impl.network;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface Dialogue {

	UUID getUuid();

	//String getRealm();

	Dialogue send(KensukePacket packet);

	<T extends KensukePacket> CompletableFuture<T> awaitFuture(Class<T> type);

	<T extends KensukePacket> T await(Class<T> type) throws KensukeException;

}
