package appbox.cache;

import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class ObjectCacheAsync<K, V> {

    private final AsyncLoadingCache<K, V> _cache;
    private       Cache<K, V>             cache;

    public ObjectCacheAsync(long maxSize, int expireAfterSeconds, Function<K, CompletableFuture<V>> loader) {
        var temp = Caffeine.newBuilder().maximumSize(maxSize);
        if (expireAfterSeconds > 0)
            temp.expireAfterAccess(expireAfterSeconds, TimeUnit.SECONDS);

        _cache = temp.buildAsync(new AsyncCacheLoader<K, V>() {
            @Override
            public @NonNull CompletableFuture<V> asyncLoad(@NonNull K key, @NonNull Executor executor) {
                return loader.apply(key);
            }
        });
    }

    public CompletableFuture<V> get(K key) {
        return _cache.get(key);
    }

    public void invalidate(K key) {
        _cache.synchronous().invalidate(key);
    }


}
