package appbox.store;

//TODO:外键引用处理考虑在存储层实现，因为可能需要实现跨进程序列化传输事务

import appbox.channel.KVRowReader;
import appbox.channel.messages.KVAddRefRequest;
import appbox.channel.messages.StoreResponse;
import appbox.data.EntityId;
import appbox.data.SysEntity;
import appbox.entities.EntityMemberValueGetter;
import appbox.logging.Log;
import appbox.model.ApplicationModel;
import appbox.model.entity.EntityRefModel;
import appbox.serialization.IEntityMemberWriter;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public final class KVTransaction implements IKVTransaction, /*IEntityMemberWriter,*/ AutoCloseable {
    private final KVTxnId                    _txnId  = new KVTxnId();
    private final AtomicInteger              _status = new AtomicInteger(0);
    private       ArrayList<KVAddRefRequest> _refs;

    private EntityMemberValueGetter _memberValueGetter;

    private KVTransaction() {}

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

    /** 减少外键引用计数值 (Delete) */
    void decEntityRef(EntityRefModel entityRef, ApplicationModel fromApp,
                      EntityId fromEntityId, byte[] rowData) {
        assert fromEntityId.raftGroupId() != 0;

        var targetId = KVRowReader.readEntityId(rowData, entityRef.getFKMemberIds()[0]);
        if (targetId == null)
            return;
        int fromTableId = KVUtil.encodeTableId(fromApp.getAppStoreId(), entityRef.owner.tableId());

        synchronized (this) {
            addEntityRefInternal(targetId, fromEntityId.raftGroupId(), fromTableId, -1);
        }
    }

    /** 更新外键引用计数值 (Update) */
    void updEntityRef(EntityRefModel entityRefModel, ApplicationModel fromApp,
                      SysEntity newEntity, byte[] oldRowData) {
        synchronized (this) {
            if (_memberValueGetter == null)
                _memberValueGetter = new EntityMemberValueGetter();
            newEntity.writeMember(entityRefModel.getFKMemberIds()[0], _memberValueGetter, IEntityMemberWriter.SF_NONE);
            var newTargetId     = (EntityId) _memberValueGetter.value;
            var oldTargetId     = KVRowReader.readEntityId(oldRowData, entityRefModel.getFKMemberIds()[0]);
            int fromTableId     = KVUtil.encodeTableId(fromApp.getAppStoreId(), entityRefModel.owner.tableId());
            var fromRaftGroupId = newEntity.id().raftGroupId();
            if (oldTargetId != null) {
                if (newTargetId != null) {
                    if (!newTargetId.equals(oldTargetId)) {
                        addEntityRefInternal(oldTargetId, fromRaftGroupId, fromTableId, -1);
                        addEntityRefInternal(newTargetId, fromRaftGroupId, fromTableId, 1);
                    }
                } else {
                    addEntityRefInternal(oldTargetId, fromRaftGroupId, fromTableId, -1);
                }
            } else if (newTargetId != null) {
                addEntityRefInternal(newTargetId, fromRaftGroupId, fromTableId, 1);
            }
        }
    }

    /** 增加外键引用计数值 (Insert) */
    void incEntityRef(EntityRefModel entityRef, ApplicationModel fromApp, SysEntity fromEntity) {
        assert fromEntity.id().raftGroupId() != 0;

        synchronized (this) {
            if (_memberValueGetter == null)
                _memberValueGetter = new EntityMemberValueGetter();
            fromEntity.writeMember(entityRef.getFKMemberIds()[0], _memberValueGetter, IEntityMemberWriter.SF_NONE);
            var targetId = (EntityId) _memberValueGetter.value;
            if (targetId == null)
                return;
            int fromTableId = KVUtil.encodeTableId(fromApp.getAppStoreId(), entityRef.owner.tableId());

            addEntityRefInternal(targetId, fromEntity.id().raftGroupId(), fromTableId, 1);
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
                task = SysStoreApi.execCommandAsync(r).thenAccept(StoreResponse::checkStoreError);
            } else {
                task = task.thenCompose(res -> SysStoreApi.execCommandAsync(r))
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
}
