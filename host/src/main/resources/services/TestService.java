package sys.Services;

import java.util.concurrent.CompletableFuture;

public final class TestService {

    public CompletableFuture<String> sayHello() {
        return CompletableFuture.completedFuture("Hello Future!");
    }

}