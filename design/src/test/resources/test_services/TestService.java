import java.util.concurrent.CompletableFuture;

public class TestService {

    public CompletableFuture<String> hello(String name, int age) {
        var obj = new sys.entities.Employee();
        obj.name = "Rick";
        obj.male = true;
        return DataStore.DemoDB.insertAsync(obj, null)
                .thenApply(r -> "Hello Future!");
        //return CompletableFuture.completedFuture("Hello Future!");
    }

}