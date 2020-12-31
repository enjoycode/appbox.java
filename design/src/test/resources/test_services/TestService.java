import java.util.concurrent.CompletableFuture;

public class TestService {

    public CompletableFuture<String> hello(String name, int age) {
        return CompletableFuture.completedFuture("Hello Future!");
    }

    //public CompletableFuture<String> insert() {
    //    var obj = new sys.entities.Employee("Rick");
    //    obj.Male = true;
    //    return obj.saveAsync().thenApply(r -> "Save Done.");
    //    //return DataStore.DemoDB.insertAsync(obj, null)
    //    //        .thenApply(r -> "Hello Future!");
    //}

    //public CompletableFuture<Void> test1() {
    //    return CompletableFuture.completedFuture((Void) null);
    //}
    //
    //public CompletableFuture<String> invoke() {
    //    var obj = new sys.entities.Employee("Rick");
    //
    //    //sys.services.TestService.test1();
    //    return sys.services.TestService.hello(obj.Name, 100);
    //}
    //
    //public CompletableFuture<Object> query() {
    //    var obj = new sys.entities.Employee();
    //
    //    var q = new SqlQuery<sys.entities.Employee>();
    //    q.where(e -> e.Name + "a" == obj.Name);
    //    return q.toListAsync().thenApply(r -> (Object) r);
    //}

    //public CompletableFuture<?> query2() {
    //    var q = new SqlQuery<sys.entities.Employee>();
    //    return q.toListAsync(r -> new Object() {
    //        final String Name = r.Name;
    //        final boolean MaleFlag = r.Male;
    //        final String Male = r.Male ? "男" : "女";
    //    });
    //}

    //public CompletableFuture<?> query3() {
    //    var q = new SqlQuery<sys.entities.Employee>();
    //    return q.toListAsync(r -> new sys.entities.Employee() {
    //                final String ParentName = r.Manager.Name;
    //            });
    //}

    //public CompletableFuture<?> update() {
    //    var cmd = new SqlUpdateCommand<sys.entities.Employee>();
    //    cmd.where(e -> e.Name == "Rick");
    //    cmd.update(e -> e.Male = true);
    //    var outs = cmd.output(e -> e.Name);
    //    return cmd.execAsync().thenApply(rows -> {
    //       return outs.get(0);
    //    });
    //}

    public CompletableFuture<?> subquery() {
        var sq = new SqlQuery<sys.entities.Employee>();
        sq.where(t -> t.ManagerName == "Rick");

        var q = new SqlQuery<sys.entities.Employee>();
        q.where(t -> DbFunc.in(t.Name, sq.toSubQuery(s -> s.Name)));
        return q.toListAsync();
    }

    //public CompletableFuture<Object> testEntityArg(sys.entities.Employee emp) {
    //    return CompletableFuture.completedFuture(emp);
    //}

}
