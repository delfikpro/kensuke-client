package ru.cristalix.core.stats;

import ru.cristalix.core.CoreApi;
import ru.cristalix.core.IService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface IStatService extends IService {

	static IStatService get() {
		return CoreApi.get().getService(IStatService.class);
	}

	void useScopes(Scope<?>... scopes);

	<T> UserManager<T> registerUserManager(UserProvider<T> provider, UserSerializer<T> serializer);

	StatContext saveUser(UUID uuid);

	void setDataRequired(boolean requireData);

	boolean isDataRequired();

	<T> CompletableFuture<List<T>> getLeaderboard(Scope<T> scope, String field, int limit);

//	CompletableFuture<StatContext> loadStatSnapshot(UUID userId, Collection<String> scopes);

//	CompletableFuture<Void> saveStatSnapshot(UUID userId, Consumer<StatContext> saver);

}
