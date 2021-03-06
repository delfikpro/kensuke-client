package ru.cristalix.core.stats.impl.network;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.cristalix.core.stats.impl.network.packet.PacketError;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Getter
@RequiredArgsConstructor
public class DialogueImpl implements Dialogue {

	private final UUID uuid;
	private final StatsWebSocketClient client;

	@Override
	public Dialogue send(StatServicePacket packet) {
		client.write(packet.toFrame(uuid));
		return this;
	}

	@Override
	public <T extends StatServicePacket> CompletableFuture<T> awaitFuture(Class<T> type) {
		CompletableFuture<T> future = new CompletableFuture<>();
		client.getResponseCache().put(uuid, future);
		return future;
	}

	@Override
	public <T extends StatServicePacket> T await(Class<T> type) throws StatServiceException {

		CompletableFuture<T> future = awaitFuture(type);

		try {
			StatServicePacket response = future.get(1, TimeUnit.SECONDS);
			if (response instanceof PacketError) {
				PacketError packetError = (PacketError) response;
				throw new StatServiceException(packetError.getErrorLevel(), packetError.getErrorMessage());
			}
			if (!type.isInstance(response)) {
				throw new StatServiceException(PacketError.ErrorLevel.FATAL, "Foreign packet type: " + response.getClass().getName() + " instead of " + type.getName());
			}
			return type.cast(response);
		} catch (TimeoutException ex) {
			throw new StatServiceException(PacketError.ErrorLevel.TIMEOUT, "Dialogue " + uuid + " timed out while waiting for " + type.getSimpleName(), ex);
		} catch (InterruptedException | ExecutionException ex) {
			throw new StatServiceException(PacketError.ErrorLevel.SEVERE, "Unknown error while waiting for " + type.getSimpleName() + " in " + uuid, ex);
		}
	}

}
