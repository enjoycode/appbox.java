import java.util.concurrent.CompletableFuture;

public class TestService {

    public CompletableFuture<String> hello(String name, int age) {
        var obj = new sys.entities.Employee();
        System.out.println(obj);
        return CompletableFuture.completedFuture("Hello Future!");
    }

}