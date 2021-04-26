package dev.implario.kensuke;

import dev.implario.kensuke.scope.Scope;
import ru.cristalix.core.CoreApi;
import ru.cristalix.core.IService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface IKensuke extends IService {

	static IKensuke get() {
		return CoreApi.get().getService(IKensuke.class);
	}

	List<Scope<?>> getScopes();

	void useScopes(Scope<?>... scopes);

	<T> UserPool<T> registerUserManager(DataDeserializer<T> provider, DataSerializer<T> serializer, Scope<?>... scopes);

	DataContext saveUser(UUID uuid);

	void setDataRequired(boolean requireData);

	boolean isDataRequired();

	<T> CompletableFuture<List<T>> getLeaderboard(Scope<T> scope, String field, int limit);

	<T> CompletableFuture<List<T>> addLeaderboardUsers(UserPool<T> userManager, Scope<T> scope, String field, int limit);

	CompletableFuture<DataContext> loadSnapshot(UUID uuid, List<Scope<?>> scopes);

}
