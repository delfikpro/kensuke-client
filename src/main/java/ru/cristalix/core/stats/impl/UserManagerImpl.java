package ru.cristalix.core.stats.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import ru.cristalix.core.stats.UserManager;
import ru.cristalix.core.stats.UserProvider;
import ru.cristalix.core.stats.UserSerializer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class UserManagerImpl<T> implements UserManager<T> {

	@Delegate
	private final UserProvider<T> provider;

	@Delegate
	private final UserSerializer<T> serializer;

	private final Map<UUID, T> userMap = new HashMap<>();

	@Override
	public T getUser(UUID uuid) {
		return this.userMap.get(uuid);
	}

	@Override
	public void addUser(UUID uuid, T user) {
		this.userMap.put(uuid, user);
	}

	@Override
	public T removeUser(UUID uuid) {
		return this.userMap.remove(uuid);
	}

}
