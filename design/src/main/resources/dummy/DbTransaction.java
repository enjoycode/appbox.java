package sys;

import java.util.concurrent.CompletableFuture;

public final class DbTransaction implements AutoCloseable {
    public CompletableFuture<Void> commitAsync() {return null;}

    @Override
    public void close() throws Exception {}
}
