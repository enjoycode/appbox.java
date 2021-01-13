import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.concurrent.CompletableFuture;

import sys.*;

@RuntimeType(type = "appbox.store.query.SqlQuery")
@CtorInterceptor(name = "SqlQuery")
public final class SqlQuery<T extends SqlEntityBase> implements ISqlIncluder<T> {

    public SqlQuery() {}

    public SqlQuery<T> skip(int rows) {return this;}

    public SqlQuery<T> take(int rows) {return this;}

    @MethodInterceptor(name = "SqlQueryWhere")
    public SqlQuery<T> where(Predicate<T> filter) {return this;}

    @MethodInterceptor(name = "SqlQueryWhere")
    public <J> SqlQuery<T> where(ISqlQueryJoin<J> join, BiPredicate<T, J> filter) {return this;}

    @MethodInterceptor(name = "SqlQueryWhere")
    public SqlQuery<T> andWhere(Predicate<T> filter) {return this;}

    @MethodInterceptor(name = "SqlQueryWhere")
    public SqlQuery<T> orWhere(Predicate<T> filter) {return this;}

    public CompletableFuture<List<T>> toListAsync() {return null;}

    @MethodInterceptor(name = "SqlQueryMapper")
    public <R> CompletableFuture<List<R>> toListAsync(Function<? super T, ? extends R> mapper) {return null;}

    @MethodInterceptor(name = "SqlQueryMapper")
    public <J, R> CompletableFuture<List<R>> toListAsync(ISqlQueryJoin<J> join,
                                                         BiFunction<? super T, J, ? extends R> mapper) {return null;}

    @MethodInterceptor(name = "SqlQuerySelect")
    public CompletableFuture<List<T>> toTreeAsync(Function<? super T, List<T>> children) {return null;}

    @MethodInterceptor(name = "SqlQuerySelect")
    public <R> SqlSubQuery<R> toSubQuery(Function<? super T, ? extends R> selects) {return null;}

    @MethodInterceptor(name = "SqlQuerySelect")
    public <R> SqlQuery<T> orderBy(Function<? super T, ? extends R> select) {return null;}

    @MethodInterceptor(name = "SqlQuerySelect")
    public <R> SqlQuery<T> orderByDesc(Function<? super T, ? extends R> select) {return null;}

    @MethodInterceptor(name = "SqlQuerySelect")
    public <R> SqlQuery<T> groupBy(Function<? super T, ? extends R> select) {return null;}

    @MethodInterceptor(name = "SqlQueryWhere")
    public SqlQuery<T> having(Predicate<T> filter) {return this;}

    @MethodInterceptor(name = "SqlQueryWhere")
    public <R> ISqlQueryJoin<R> leftJoin(ISqlQueryJoin<R> target, BiPredicate<T, R> condition) {return null;}

    @MethodInterceptor(name = "SqlQueryWhere")
    public <R> ISqlQueryJoin<R> innerJoin(ISqlQueryJoin<R> target, BiPredicate<T, R> condition) {return null;}

    @MethodInterceptor(name = "SqlQueryWhere")
    public <R> ISqlQueryJoin<R> rightJoin(ISqlQueryJoin<R> target, BiPredicate<T, R> condition) {return null;}

    @MethodInterceptor(name = "SqlQueryWhere")
    public <R> ISqlQueryJoin<R> fullJoin(ISqlQueryJoin<R> target, BiPredicate<T, R> condition) {return null;}

}
