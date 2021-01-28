import appbox.cache.ObjectCacheAsync;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

public class TestObjectCache {

    @Test
    public void testCache() throws ExecutionException, InterruptedException {
        var cache = new ObjectCacheAsync<String, String>(10, 0, key -> {
            if (key.equals("None")) {
                //return CompletableFuture.failedFuture(new RuntimeException("Not exists"));
                return CompletableFuture.completedFuture(null);
            }
            return CompletableFuture.completedFuture(key);
        });

        var v1 = cache.get("None").get();
        assertNull(v1);
        var v2 = cache.get("None").get();
        assertNull(v2);

        var v3 = cache.get("A").get();
        var v4 = cache.get("A").get();
        assertEquals(v3, v4);

        cache.invalidate("A");
        var v5 = cache.get("A").get();
        assertEquals(v3, v5);
    }

}
