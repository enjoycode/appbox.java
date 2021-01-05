import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.concurrent.CompletableFuture;

import sys.*;

@RuntimeType(type = "appbox.store.query.SqlQuery")
@CtorInterceptor(name = "SqlQuery")
public final class SqlQuery<T extends SqlEntityBase> {

    public SqlQuery() {}

    @MethodInterceptor(name = "SqlQueryWhere")
    public SqlQuery<T> where(Predicate<T> filter) {return this;}

    @MethodInterceptor(name = "SqlQueryWhere")
    public <J> SqlQuery<T> where(ISqlQueryJoin<J> join, BiPredicate<T, J> filter) {return this;}

    public CompletableFuture<List<T>> toListAsync() { return null; }

    @MethodInterceptor(name = "SqlQueryMapper")
    public <R> CompletableFuture<List<R>> toListAsync(Function<? super T, ? extends R> mapper) {return null;}

    @MethodInterceptor(name = "SqlQueryMapper")
    public <J, R> CompletableFuture<List<R>> toListAsync(ISqlQueryJoin<J> join,
                                                         BiFunction<? super T, J, ? extends R> mapper) {return null;}

    @MethodInterceptor(name = "SqlQuerySelect")
    public <R> SqlSubQuery<R> toSubQuery(Function<? super T, ? extends R> selects) {return null;}

    @MethodInterceptor(name = "SqlQueryWhere")
    public <R> ISqlQueryJoin<R> leftJoin(ISqlQueryJoin<R> target, BiPredicate<T, R> condition) {
        return null;
    }

    @MethodInterceptor(name = "SqlQueryWhere")
    public <R> ISqlQueryJoin<R> innerJoin(ISqlQueryJoin<R> target, BiPredicate<T, R> condition) {
        return null;
    }

    @MethodInterceptor(name = "SqlQueryWhere")
    public <R> ISqlQueryJoin<R> rightJoin(ISqlQueryJoin<R> target, BiPredicate<T, R> condition) {
        return null;
    }

    @MethodInterceptor(name = "SqlQueryWhere")
    public <R> ISqlQueryJoin<R> fullJoin(ISqlQueryJoin<R> target, BiPredicate<T, R> condition) {
        return null;
    }

}
