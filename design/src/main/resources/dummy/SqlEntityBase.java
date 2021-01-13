package sys;

import appbox.store.DbTransaction;

import java.util.concurrent.CompletableFuture;

public abstract class SqlEntityBase extends EntityBase {

    /** 标记实体为删除状态 */
    public final void markDeleted() {}

    /** 根据实体持久化状态保存 */
    @MethodInterceptor(name = "SaveEntity")
    public final CompletableFuture<Void> saveAsync() {return null;}

    /** 根据实体持久化状态在事务内保存 */
    @MethodInterceptor(name = "SaveEntity")
    public final CompletableFuture<Void> saveAsync(DbTransaction txn) {return null;}

}
