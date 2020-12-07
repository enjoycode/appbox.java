import java.util.concurrent.CompletableFuture;

public class TestService {

    public CompletableFuture<String> hello(String name, int age) {
        var obj = new sys.entities.Employee();
        obj.name = "Rick";
        obj.male = true;
        System.out.println(obj.name);
        return CompletableFuture.completedFuture("Hello Future!");
    }

}