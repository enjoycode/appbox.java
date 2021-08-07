import java.util.concurrent.CompletableFuture;
import java.util.List;

import static sys.Async.await;

public class TestService {

    private final long modelId = sys.entities.Employee.MODELID;

    private void test(sys.entities.Employee a, sys.entities.Employee b) {
        if (!a.Name.equals(b.Name))
            a.Age = b.Age;
        a.Manager.Age = b.Manager.Age + 1;
        get().Age         = b.Age;
        get().Manager.Age = b.Age + 1;
        a.Age             = get().Manager.Age;
    }

    private sys.entities.Employee get() {
        return null;
    }

    public CompletableFuture<?> testAwait() {
        if (!sys.Permissions.Admin)
            throw new RuntimeException("无Admin权限");

        var res = await(query());
        return CompletableFuture.completedFuture(res.get(0).Name);
    }

    public CompletableFuture<String> hello(String name, int age) {
        return CompletableFuture.completedFuture("Hello Future!");
    }

    public CompletableFuture<String> insert() {
        var obj = new sys.entities.Employee("Rick");
        obj.Male = true;
        return obj.saveAsync().thenApply(r -> "Save Done.");
        //return DataStore.DemoDB.insertAsync(obj, null)
        //        .thenApply(r -> "Hello Future!");
    }

    public CompletableFuture<Void> test1() {
        return CompletableFuture.completedFuture((Void) null);
    }

    public CompletableFuture<String> invoke() {
        var obj = new sys.entities.Employee("Rick");

        //sys.services.TestService.test1();
        return sys.services.TestService.hello(obj.Name, 100);
    }

    public CompletableFuture<List<sys.entities.Employee>> query() {
        var obj   = new sys.entities.Employee();
        int value = 100;

        var q = new SqlQuery<sys.entities.Employee>();
        q.where(e -> e.Age + e.Age * 1 > 100);
        q.where(e -> e.Age + 1 * value > 100);
        q.where(e -> e.Manager.Name + "a" == obj.Name + "b");
        q.where(e -> DbFunc.sum(e.Age) + 1 > value + 2);
        q.where(e -> e.Name + obj.Name + "c" == "Rick");
        return q.toListAsync();
    }

    public CompletableFuture<?> query2() {
        var q = new SqlQuery<sys.entities.Employee>();
        return q.toListAsync(r -> new Object() {
            final String Name = r.Name;
            final boolean MaleFlag = r.Male;
            final String Male = r.Male ? "男" : "女";
        });
    }

    public CompletableFuture<?> query3() {
        var q = new SqlQuery<sys.entities.Employee>();
        return q.toListAsync(r -> new sys.entities.Employee() {
                    final String ParentName = r.Manager.Name;
                });
    }

    public CompletableFuture<?> update() {
        var cmd = new SqlUpdateCommand<sys.entities.Employee>();
        cmd.where(e -> e.Name == "Rick");
        cmd.update(e -> e.Male = true);
        var outs = cmd.output(e -> e.Name);
        return cmd.execAsync().thenApply(rows -> {
           return outs.get(0);
        });
    }

    public CompletableFuture<?> subquery() {
        var sq = new SqlQuery<sys.entities.Employee>();
        sq.where(t -> t.ManagerName == "Rick");

        var q = new SqlQuery<sys.entities.Employee>();
        q.where(t -> t.Age > 30 && DbFunc.in(t.Name, sq.toSubQuery(s -> s.Name)) );
        return q.toListAsync();
    }

    public CompletableFuture<?> join() {
        var q = new SqlQuery<sys.entities.Employee>();
        var j = new SqlQueryJoin<sys.entities.Employee>();

        q.leftJoin(j, (l, r) -> l.ManagerName == r.Name);
        q.where(j, (l, r) -> r.Name == "Rick");
        return q.toListAsync();
    }

    public CompletableFuture<?> groupBy() {
        var q = new SqlQuery<sys.entities.Employee>();
        q.groupBy(t -> t.ManagerName)
                .having(t -> DbFunc.sum(t.Age) + 3 > 0);
        return q.toListAsync(t -> new Object() {
            final String manager = t.ManagerName;
            final int ages = DbFunc.sum(t.Age + 1) + 2;
        });
    }

    public CompletableFuture<?> testEntityId(appbox.data.EntityId orderId) {
        return CompletableFuture.completedFuture(orderId);
    }

    public CompletableFuture<Object> testEntityArg(sys.entities.Employee emp) {
        return CompletableFuture.completedFuture(emp);
    }

}
