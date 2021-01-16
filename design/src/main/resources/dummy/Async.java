package sys;

import java.util.concurrent.CompletionStage;

public final class Async {
    @MethodInterceptor(name = "AsyncAwait")
    public static <T, F extends CompletionStage<T>> T await(F future) {
        throw new RuntimeException();
    }
}
