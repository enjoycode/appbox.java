package sys;

import java.util.concurrent.CompletableFuture;

@RuntimeType(type = "appbox.store.KVTransaction")
public final class KVTransaction {
    private KVTransaction() {}

    @MethodInterceptor(name = "InvokeStatic")
    public static CompletableFuture<KVTransaction> beginAsync(/*TODO: isoLevel*/) {return null;}

    public CompletableFuture<Void> commitAsync() {return null;}

    public void rollback() {}
}
