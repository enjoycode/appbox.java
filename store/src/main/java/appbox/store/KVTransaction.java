package appbox.store;

//TODO:外键引用处理考虑在存储层实现，因为可能需要实现跨进程序列化传输事务

import appbox.channel.KVRowReader;
import appbox.channel.messages.KVAddRefRequest;
import appbox.channel.messages.StoreResponse;
import appbox.data.Entity;
import appbox.data.EntityId;
import appbox.data.SysEntity;
import appbox.logging.Log;
import appbox.model.ApplicationModel;
import appbox.model.entity.EntityRefModel;
import appbox.serialization.IEntityMemberWriter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public final class KVTransaction implements IKVTransaction, IEntityMemberWriter, AutoCloseable {
    private final KVTxnId                    _txnId  = new KVTxnId();
    private final AtomicInteger              _status = new AtomicInteger(0);
    private       ArrayList<KVAddRefRequest> _refs;
    private       EntityId                   _tempTargetId; //外键引用的目标实体标识
    //private       long                       _tempTypeModelId;

    private KVTransaction() {
    }

    @Override
    public KVTxnId id() {
        return _txnId;
    }

    //region ====begin & commit & rollback====
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

        //递交前先处理挂起的外键引用
        return execPendingRefs()
                .thenCompose(r -> SysStoreApi.commitTxnAsync(_txnId))
                .thenAccept(StoreResponse::checkStoreError);
    }

    public void rollback() {
        if (_status.compareAndExchange(0, 2) != 0) {
            Log.debug("Transaction has commit or rollback.");
            return;
        }

        SysStoreApi.rollbackTxnAsync(_txnId).thenAccept(r -> {
            if (r.errorCode != 0) {
                Log.warn("回滚事务出现异常，暂忽略");
            }
        });
    }

    /**
     * 用于CompletableFuture.whenComplete时检测是否异常自动回滚事务
     * @param ex 不为null则回滚事务
     */
    void rollbackOnException(Throwable ex) {
        if (ex != null)
            rollback();
    }
    //endregion

    //region ====外键引用相关====

    /** 减少外键引用计数值 (Update or Delete) */
    void decEntityRef(EntityRefModel entityRef, ApplicationModel fromApp,
                      EntityId fromEntityId, byte[] rowData) {
        assert fromEntityId.raftGroupId() != 0;

        synchronized (this) {
            _tempTargetId = KVRowReader.readEntityId(rowData, entityRef.getFKMemberIds()[0]);
            if (_tempTargetId == null)
                return;
            int fromTableId = KeyUtil.encodeTableId(fromApp.getAppStoreId(), entityRef.owner.tableId());

            addEntityRefInternal(_tempTargetId, fromEntityId.raftGroupId(), fromTableId, -1);
        }
    }

    /** 增加外键引用计数值 (Insert时) */
    void incEntityRef(EntityRefModel entityRef, ApplicationModel fromApp, SysEntity fromEntity) {
        assert fromEntity.id().raftGroupId() != 0;

        synchronized (this) {
            fromEntity.writeMember(entityRef.getFKMemberIds()[0], this, IEntityMemberWriter.SF_NONE);
            if (_tempTargetId == null)
                return;
            int fromTableId = KeyUtil.encodeTableId(fromApp.getAppStoreId(), entityRef.owner.tableId());

            addEntityRefInternal(_tempTargetId, fromEntity.id().raftGroupId(), fromTableId, 1);
        }
    }

    private void addEntityRefInternal(EntityId targetEntityId, long fromRaftGroupId, int fromTableId, int diff) {
        var found = false;
        if (_refs == null) {
            _refs = new ArrayList<>();
        } else {
            for (var it : _refs) {
                if (it.targetEntityId.equals(targetEntityId) && it.fromRaftGroupId == fromRaftGroupId) {
                    it.addDiff(diff);
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            var item = new KVAddRefRequest(_txnId, targetEntityId, fromRaftGroupId, fromTableId, diff);
            _refs.add(item);
        }
    }

    /** 处理缓存的外键引用，执行后清空 */
    CompletableFuture<Void> execPendingRefs() {
        if (_refs == null || _refs.size() <= 0) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> task = null;
        for (var r : _refs) {
            if (r.getDiff() == 0) //可能会抵消为0
                continue;

            if (task == null) {
                task = SysStoreApi.execKVAddRefAsync(r)
                        .thenAccept(StoreResponse::checkStoreError);
            } else {
                task = task.thenCompose(res -> SysStoreApi.execKVAddRefAsync(r))
                        .thenAccept(StoreResponse::checkStoreError);
            }
        }

        _refs.clear(); //别忘了清空
        return task;
    }
    //endregion

    @Override
    public void close() {
        rollback();
    }

    //region ====IEntityMemberWriter 实现此接口仅为获取引用目标的EntityId或聚合类型====
    @Override
    public void writeMember(short id, EntityId value, byte flags) {
        _tempTargetId = value; //maybe null
    }

    @Override
    public void writeMember(short id, long value, byte flags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeMember(short id, String value, byte flags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeMember(short id, byte value, byte flags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeMember(short id, int value, byte flags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeMember(short id, Integer value, byte flags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeMember(short id, UUID value, byte flags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeMember(short id, byte[] data, byte flags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeMember(short id, boolean male, byte flags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeMember(short id, Date value, byte flags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeMember(short id, Entity value, byte flags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeMember(short id, List<Entity> value, byte flags) {
        throw new UnsupportedOperationException();
    }
    //endregion

}
