import sys.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@RuntimeType(type = "appbox.store.query.TableScan")
@CtorInterceptor(name = "SqlQuery")
public final class TableScan<T extends SysEntityBase> {
    public TableScan<T> skip(int rows) {return this;}

    public TableScan<T> take(int rows) {return this;}

    public CompletableFuture<List<T>> toListAsync() {return null;}

    @MethodInterceptor(name = "SqlQuerySelect")
    public CompletableFuture<List<T>> toTreeAsync(Function<? super T, List<T>> children) {return null;}
}
