package sys;

import java.util.List;
import java.util.function.Function;

public interface ISqlIncluder<T> {
    @MethodInterceptor(name = "SqlQuerySelect")
    default <P> ISqlIncludable<T, P> include(Function<T, P> property) {return null;}

    @MethodInterceptor(name = "SqlQuerySelect")
    default <P> ISqlIncludable<T, P> includeAll(Function<T, List<P>> property) {return null;}
}
