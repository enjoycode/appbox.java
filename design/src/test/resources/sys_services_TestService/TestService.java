import java.util.concurrent.CompletableFuture;

public class TestService {

    public CompletableFuture<String> hello() {
        return CompletableFuture.completedFuture("Hello Future!");
    }

}