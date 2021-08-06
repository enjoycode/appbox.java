package sys;

import appbox.store.DbTransaction;

import java.util.concurrent.CompletableFuture;

public abstract class SqlEntityBase extends EntityBase {

    @MethodInterceptor(name = "SaveEntity")
    public final CompletableFuture<Void> insertAsync() {return null;}

    @MethodInterceptor(name = "SaveEntity")
    public final CompletableFuture<Void> insertAsync(DbTransaction txn) {return null;}

    @MethodInterceptor(name = "SaveEntity")
    public final CompletableFuture<Void> updateAsync() {return null;}

    @MethodInterceptor(name = "SaveEntity")
    public final CompletableFuture<Void> updateAsync(DbTransaction txn) {return null;}

    @MethodInterceptor(name = "SaveEntity")
    public final CompletableFuture<Void> deleteAsync() {return null;}

    @MethodInterceptor(name = "SaveEntity")
    public final CompletableFuture<Void> deleteAsync(DbTransaction txn) {return null;}

    /** 标记实体为删除状态 */
    @Deprecated
    public final void markDeleted() {}

    /** 根据实体持久化状态保存 */
    @MethodInterceptor(name = "SaveEntity")
    @Deprecated
    public final CompletableFuture<Void> saveAsync() {return null;}

    /** 根据实体持久化状态在事务内保存 */
    @MethodInterceptor(name = "SaveEntity")
    @Deprecated
    public final CompletableFuture<Void> saveAsync(DbTransaction txn) {return null;}

}
