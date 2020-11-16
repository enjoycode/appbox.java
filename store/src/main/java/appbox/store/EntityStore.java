package appbox.store;

import appbox.channel.messages.*;
import appbox.data.EntityId;
import appbox.data.SysEntity;
import appbox.model.ApplicationModel;
import appbox.model.EntityModel;
import appbox.runtime.RuntimeContext;
import appbox.store.caching.MetaCaches;

import java.util.concurrent.CompletableFuture;

public final class EntityStore { //TODO: rename to SysStore

    //region ====分区信息及缓存====
    private static CompletableFuture<Long> tryGetPartitionByReadIndex(PartitionInfo partitionInfo) {
        var req = new KVGetPartitionRequest(partitionInfo);
        return SysStoreApi.execKVGetAsync(req, new KVGetPartitionResponse()).thenApply(r -> {
            //这里暂不判断没有取到，由调用者处理
            if (r.raftGroupId != 0) {
                MetaCaches.tryAddPartition(partitionInfo, r.raftGroupId); //加入本地缓存
            }
            return r.raftGroupId;
        });
    }

    private static CompletableFuture<Long> tryCreatePartition(PartitionInfo partitionInfo, IKVTransaction txn) {
        return SysStoreApi.metaGenPartitionAsync(partitionInfo, txn.id()).thenApply(r -> {
            if (r.errorCode != 0) {
                throw new SysStoreException(r.errorCode);
            } else {
                MetaCaches.tryAddPartition(partitionInfo, r.raftGroupId); //加入本地缓存
                return r.raftGroupId;
            }
        });
    }

    /**
     * 获取或创建全局表的RaftGroupId
     * @param txn 如果为null,则表示不需要创建
     */
    public static CompletableFuture<Long> getOrCreateGlobalTablePartition(
            ApplicationModel app, EntityModel model, IKVTransaction txn) {
        var partitionInfo = new PartitionInfo(5, model.sysStoreOptions().tableFlags());
        partitionInfo.encodeGlobalTablePartitionKey(app.getAppStoreId(), model.tableId());

        //先查询本地缓存是否存在
        Long raftGroupId = MetaCaches.tryGetPartition(partitionInfo);
        if (raftGroupId != null) {
            return CompletableFuture.completedFuture(raftGroupId);
        }

        //暂根据是否在事务判断是否需要创建
        if (txn != null) {
            return tryCreatePartition(partitionInfo, txn);
        } else {
            return tryGetPartitionByReadIndex(partitionInfo);
        }
    }
    //endregion

    //region ====实体及索引相关操作====
    //TODO:*** Insert/Update/Delete本地索引及数据通过BatchCommand优化，减少RPC次数

    //region ----Insert----
    public static CompletableFuture<Void> insertEntityAsync(SysEntity entity) {
        return insertEntityAsync(entity, false);
    }

    public static CompletableFuture<Void> insertEntityAsync(SysEntity entity, boolean overrideExists) {
        return KVTransaction.beginAsync()
                .thenCompose(txn -> insertEntityAsync(entity, txn, overrideExists)
                        .thenCompose(r -> txn.commitAsync()));
    }

    public static CompletableFuture<Void> insertEntityAsync(SysEntity entity, KVTransaction txn) {
        return insertEntityAsync(entity, txn, false);
    }

    public static CompletableFuture<Void> insertEntityAsync(SysEntity entity, KVTransaction txn, boolean overrideExists) {
        //TODO:考虑自动新建事务, 分区已存在且模型没有索引没有关系则可以不需要事务
        if (txn == null)
            throw new RuntimeException("Must enlist transaction");
        return insertEntityInternal(entity, txn, overrideExists)
                .whenComplete((r, ex) -> txn.rollbackOnException(ex));
    }

    private static CompletableFuture<Void> insertEntityInternal(SysEntity entity, KVTransaction txn, boolean overrideExists) {
        //TODO:判断模型运行时及持久化状态
        var model = entity.model(); //肯定存在，不需要RuntimeContext.Current.GetEntityModel
        //暂不允许没有成员的插入操作
        if (model.getMembers().size() == 0)
            throw new RuntimeException("Entity[{model.Name}] has no member");
        //暂不允许override非MVCC的记录
        if (overrideExists && model.sysStoreOptions().isMVCC())
            throw new RuntimeException("Can't override exists with MVCC");
        var app = RuntimeContext.current().getApplicationModel(model.appId());

        //根据是否分区定位
        CompletableFuture<Long> getRaftGroupIdTask;
        if (model.sysStoreOptions().hasPartitionKeys()) {
            throw new RuntimeException("未实现");
        } else {
            getRaftGroupIdTask = getOrCreateGlobalTablePartition(app, model, txn);
        }

        return getRaftGroupIdTask.thenCompose(raftGroupId -> {
            if (raftGroupId == 0)
                throw new RuntimeException("Can't get or create partition.");

            //设置EntityId's raftGroupId
            entity.id().initRaftGroupId(raftGroupId);

            //TODO:判断有无强制外键引用，有则先处理
            //插入索引，注意模型变更后可能已添加或删除了索引会报错
            return insertIndexesAsync(entity, model, txn);
        }).thenCompose(r -> {
            //插入数据
            var req = new KVInsertEntityRequest(entity, model, txn.id()); //TODO: refs
            req.overrideExists = overrideExists;
            return SysStoreApi.execKVInsertAsync(req);
        }).thenAccept(StoreResponse::checkStoreError);
    }
    //endregion insert

    //region ----Delete----
    public static CompletableFuture<Void> deleteEntityAsync(EntityModel model, EntityId id, KVTransaction txn) {
        if (txn == null)
            throw new RuntimeException("Must enlist transaction");
        if (id == null || model == null)
            throw new IllegalArgumentException();

        //注意删除前先处理本事务挂起的外键引用，以防止同一事务删除引用后再删除引用目标失败(eg:同一事务删除订单明细，删除引用的订单)
        //await txn.ExecPendingRefs();

        var app = RuntimeContext.current().getApplicationModel(model.appId());
        //先获取强制外键引用
        //var refs = model.GetEntityRefsWithFKConstraint();

        //删除数据
        var req = new KVDeleteEntityRequest(txn.id(), id, model);
        return SysStoreApi.execKVDeleteAsync(req)
                .thenAccept(res -> {
                    //TODO://删除索引并扣减引用计数
                });
    }
    //endregion delete

    //region ----Index----

    private static CompletableFuture<Void> insertIndexesAsync(SysEntity entity, EntityModel model, KVTransaction txn) {
        if (!model.sysStoreOptions().hasIndexes()) {
            return CompletableFuture.completedFuture(null);
        }

        //TODO:并发插入索引，暂顺序处理，可考虑先处理惟一索引
        //TODO:暂只处理分区本地索引
        CompletableFuture<KVCommandResponse> fut = null;
        for (var idx : model.sysStoreOptions().getIndexes()) {
            if (idx.isGlobal()) {
                throw new RuntimeException("未实现");
            }

            var req = new KVInsertIndexRequest(txn.id(), entity, idx);
            if (fut == null)
                fut = SysStoreApi.execKVInsertAsync(req);
            else
                fut = fut.thenCompose(r -> SysStoreApi.execKVInsertAsync(req));
        }

        return fut.thenAccept(StoreResponse::checkStoreError);
    }

    //endregion index

    //endregion

}
