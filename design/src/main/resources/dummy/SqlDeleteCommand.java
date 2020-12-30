import sys.*;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@RuntimeType(type = "appbox.store.query.SqlDeleteCommand")
@CtorInterceptor(name = "SqlQuery")
public final class SqlDeleteCommand<T extends SqlEntityBase> {

    public SqlDeleteCommand() {}

    @MethodInterceptor(name = "SqlQueryWhere")
    public SqlDeleteCommand<T> where(Predicate<T> filter) {return this;}

    public CompletableFuture<Long> execAsync() { return null; }

    public CompletableFuture<Long> execAsync(DbTransaction txn) { return null; }

}
