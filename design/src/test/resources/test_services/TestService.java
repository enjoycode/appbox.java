import java.util.concurrent.CompletableFuture;

public class TestService {

    public CompletableFuture<String> hello(String name, int age) {
        return CompletableFuture.completedFuture("Hello Future!");
    }

    //public CompletableFuture<String> insert() {
    //    var obj = new sys.entities.Employee();
    //    obj.name = "Rick";
    //    obj.male = true;
    //    return DataStore.DemoDB.insertAsync(obj, null)
    //            .thenApply(r -> "Hello Future!");
    //}
    //
    //public CompletableFuture<Void> test1() {
    //    return CompletableFuture.completedFuture((Void) null);
    //}
    //
    //public CompletableFuture<String> invoke() {
    //    var obj = new sys.entities.Employee();
    //    obj.name = "Rick";
    //
    //    //sys.services.TestService.test1();
    //    return sys.services.TestService.hello(obj.name, 100);
    //}
    //
    //public CompletableFuture<Object> query() {
    //    var obj = new sys.entities.Employee();
    //
    //    var q = new SqlQuery<sys.entities.Employee>();
    //    q.where(e -> e.name + "a" == obj.name);
    //    return q.toListAsync().thenApply(r -> (Object) r);
    //}

    public CompletableFuture<?> query2() {
        var q = new SqlQuery<sys.entities.Employee>();
        return q.toListAsync(r -> new Object() {
            final String Name = r.name;
            final boolean MaleFlag = r.male;
            final String Male = r.male ? "男" : "女";
        });
    }

    public CompletableFuture<Object> testEntityArg(sys.entities.Employee emp) {
        return CompletableFuture.completedFuture(emp);
    }

}
