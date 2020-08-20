package appbox.store;

//TODO:外键引用处理考虑在存储层实现，因为可能需要实现跨进程序列化传输事务

import appbox.logging.Log;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public final class KVTransaction implements AutoCloseable {
    final class RefFromItem {

    }

    private final KVTxnId       _txnId  = new KVTxnId();
    private final AtomicInteger _status = new AtomicInteger(0);

    private KVTransaction() {
    }

    public static CompletableFuture<KVTransaction> beginAsync(/*TODO: isoLevel*/) {
        return SysStoreApi.beginTxnAsync().thenApply(res -> {
            if (res.errorCode != 0) {
                throw new SysStoreException(res.errorCode);
            }

            var txn = new KVTransaction();
            txn._txnId.copyFrom(res.txnId);
            return txn;
        });
    }

    public CompletableFuture<Void> commitAsync() {
        if (_status.compareAndExchange(0, 1) != 0) {
            throw new RuntimeException("KVTransaction has committed or rollback");
        }

        //TODO:递交前先处理挂起的外键引用
        return SysStoreApi.commitTxnAsync(_txnId).thenAccept(r -> {
            if (r.errorCode != 0) {
                throw new SysStoreException(r.errorCode);
            }
        });
    }

    public void rollback() {
        if (_status.compareAndExchange(0, 2) != 0) {
            return;
        }

        SysStoreApi.rollbackTxnAsync(_txnId).thenAccept(r -> {
            if (r.errorCode != 0) {
                Log.warn("回滚事务出现异常，暂忽略");
            }
        });
    }

    @Override
    public void close() throws Exception {
        rollback();
    }
}
