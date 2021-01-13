package sys;

import java.util.List;
import java.util.function.Function;

public interface ISqlIncludable<T, P> extends ISqlIncluder<T> {
    @MethodInterceptor(name = "SqlQuerySelect")
    default <R> ISqlIncludable<T, R> thenInclude(Function<P, R> property) {return null;}

    @MethodInterceptor(name = "SqlQuerySelect")
    default <R> ISqlIncludable<T, R> thenIncludeAll(Function<P, List<R>> property) {return null;}
}
