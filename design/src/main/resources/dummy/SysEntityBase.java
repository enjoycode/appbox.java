package sys;

import appbox.data.EntityId;

import java.util.concurrent.CompletableFuture;

public abstract class SysEntityBase extends EntityBase {
    public final EntityId id() { return null; }

    @MethodInterceptor(name = "SaveEntity")
    public final CompletableFuture<Void> insertAsync() {return null;}

    @MethodInterceptor(name = "SaveEntity")
    public final CompletableFuture<Void> insertAsync(KVTransaction txn) {return null;}

    @MethodInterceptor(name = "SaveEntity")
    public final CompletableFuture<Void> updateAsync() {return null;}

    @MethodInterceptor(name = "SaveEntity")
    public final CompletableFuture<Void> updateAsync(KVTransaction txn) {return null;}

    @MethodInterceptor(name = "SaveEntity")
    public final CompletableFuture<Void> deleteAsync() {return null;}

    @MethodInterceptor(name = "SaveEntity")
    public final CompletableFuture<Void> deleteAsync(KVTransaction txn) {return null;}

    /** 根据实体持久化状态保存 */
    @MethodInterceptor(name = "SaveEntity")
    @Deprecated
    public final CompletableFuture<Void> saveAsync() {return null;}

    /** 根据实体持久化状态在事务内保存 */
    @MethodInterceptor(name = "SaveEntity")
    @Deprecated
    public final CompletableFuture<Void> saveAsync(KVTransaction txn) {return null;}
}
