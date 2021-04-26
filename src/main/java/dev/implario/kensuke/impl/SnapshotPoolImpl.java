package dev.implario.kensuke.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import dev.implario.kensuke.DataDeserializer;
import dev.implario.kensuke.IKensuke;
import dev.implario.kensuke.SnapshotPool;
import dev.implario.kensuke.scope.Scope;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class SnapshotPoolImpl<T> implements SnapshotPool<T> {

    @Delegate
    private final DataDeserializer<T> provider;
    @Getter
    private final List<Scope<?>> scopes;

    private final LoadingCache<UUID, T> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build(new CacheLoader<UUID, T>() {
                @Override
                public T load(@Nonnull UUID uuid) throws Exception {
                    IKensuke kensuke = IKensuke.get();
                    List<Scope<?>> scopes = getScopes().isEmpty() ? kensuke.getScopes() : getScopes();
                    return kensuke.loadSnapshot(uuid, scopes)
                            .thenApply(provider::createUser)
                            .get();
                }
            });

    @Override
    public CompletableFuture<T> getSnapshot(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return cache.get(uuid);
            } catch (ExecutionException exception) {
                throw new IllegalStateException(exception);
            }
        });
    }

    @Override
    public T getSnapshotNow(UUID uuid) {
        return cache.getIfPresent(uuid);
    }

    @Override
    public void addSnapshot(UUID uuid, T snapshot) {
        cache.put(uuid, snapshot);
    }

    @Override
    public T removeSnapshot(UUID uuid) {
        return cache.asMap().remove(uuid);
    }

    @Override
    public boolean isSnapshotLoaded(UUID uuid) {
        return cache.asMap().containsKey(uuid);
    }

    @Override
    public Map<UUID, T> getSnapshotCache() {
        return cache.asMap();
    }

}
