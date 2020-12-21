
import java.util.List;
//import java.util.function.Function;
//import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
//import java.util.function.BiPredicate;
import java.util.concurrent.CompletableFuture;

import sys.*;

@RuntimeType(type = "appbox.store.query.SqlQuery")
@CtorInterceptor(name = "SqlQuery")
public final class SqlQuery<T extends SqlEntityBase> {

    public SqlQuery() {}

    @MethodInterceptor(name = "SqlQueryWhere")
    public SqlQuery<T> where(Predicate<T> filter) {return this;}

    public CompletableFuture<List<T>> toListAsync() { return null; }

    @MethodInterceptor(name = "SqlQueryMapper")
    public <R> CompletableFuture<List<R>> toListAsync(Function<? super T, ? extends R> mapper) {return null;}

}
