package dev.implario.kensuke.impl;

import dev.implario.kensuke.DataDeserializer;
import dev.implario.kensuke.DataSerializer;
import dev.implario.kensuke.UserPool;
import dev.implario.kensuke.scope.Scope;
import lombok.experimental.Delegate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UserPoolImpl<T> extends SnapshotPoolImpl<T> implements UserPool<T> {

	@Delegate
	private final DataDeserializer<T> provider;
	@Delegate
	private final DataSerializer<T> serializer;

	private final Map<UUID, T> cache = new HashMap<>();

	public UserPoolImpl(DataDeserializer<T> provider, DataSerializer<T> serializer, List<Scope<?>> scopes) {
		super(provider, scopes);
		this.provider = provider;
		this.serializer = serializer;
	}

	@Override
	public T getUser(UUID uuid) {
		return cache.get(uuid);
	}

	@Override
	public void addUser(UUID uuid, T user) {
		cache.put(uuid, user);
	}

	@Override
	public T removeUser(UUID uuid) {
		return cache.remove(uuid);
	}

	@Override
	public boolean isUserLoaded(UUID uuid) {
		return cache.containsKey(uuid);
	}

	@Override
	public Map<UUID, T> getUserCache() {
		return cache;
	}
}
