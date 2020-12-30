
import sys.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@RuntimeType(type = "appbox.store.query.SqlUpdateCommand")
@CtorInterceptor(name = "SqlQuery")
public final class SqlUpdateCommand<T extends SqlEntityBase> {

    public SqlUpdateCommand() {}

    @MethodInterceptor(name = "SqlQueryWhere")
    public SqlUpdateCommand<T> where(Predicate<T> filter) {return this;}

    @MethodInterceptor(name = "SqlUpdateSet")
    public SqlUpdateCommand<T> update(Consumer<T> setter) {return this;}

    @MethodInterceptor(name = "SqlUpdateOut")
    public <R> UpdateOutputs<R> output(Function<T, R> selector) {return null;}

    public CompletableFuture<Long> execAsync() { return null; }

    public CompletableFuture<Long> execAsync(DbTransaction txn) { return null; }

    @RuntimeType(type = "appbox.store.query.SqlUpdateCommand.UpdateOutputs")
    public static final class UpdateOutputs<R> {
        private UpdateOutputs() {}

        public R get(int index) {
            return null;
        }

        public int size() { return 0;}
    }

}
