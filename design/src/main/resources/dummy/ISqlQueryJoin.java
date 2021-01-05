package sys;


import java.util.function.BiPredicate;

public interface ISqlQueryJoin<T> {

    @MethodInterceptor(name = "SqlQueryWhere")
    default <R> ISqlQueryJoin<R> leftJoin(ISqlQueryJoin<R> target, BiPredicate<T, R> condition) {
        return null;
    }

    @MethodInterceptor(name = "SqlQueryWhere")
    default <R> ISqlQueryJoin<R> innerJoin(ISqlQueryJoin<R> target, BiPredicate<T, R> condition) {
        return null;
    }

    @MethodInterceptor(name = "SqlQueryWhere")
    default <R> ISqlQueryJoin<R> rightJoin(ISqlQueryJoin<R> target, BiPredicate<T, R> condition) {
        return null;
    }

    @MethodInterceptor(name = "SqlQueryWhere")
    default <R> ISqlQueryJoin<R> fullJoin(ISqlQueryJoin<R> target, BiPredicate<T, R> condition) {
        return null;
    }

}
