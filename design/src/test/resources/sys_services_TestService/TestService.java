import java.util.concurrent.CompletableFuture;

public class TestService {

    public CompletableFuture<String> hello(String name, int age) {
        return CompletableFuture.completedFuture("Hello Future!");
    }

}