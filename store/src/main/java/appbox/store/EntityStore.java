package appbox.store;

import appbox.channel.messages.KVGetPartitionRequest;
import appbox.channel.messages.KVInsertEntityRequest;
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
        return SysStoreApi.execKVGetAsync(req).thenApply(r -> {
            var raftGroupId = (Long) r.result;
            if (raftGroupId != null) {
                MetaCaches.tryAddPartition(partitionInfo, raftGroupId); //加入本地缓存
            }
            return raftGroupId;
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
    public static CompletableFuture<Long> getOrCreateGlobalTablePartition(ApplicationModel app, EntityModel model, IKVTransaction txn) {
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

    public static CompletableFuture<Void> insertEntityAsync(SysEntity entity, KVTransaction txn) {
        //TODO:考虑自动新建事务, 分区已存在且模型没有索引没有关系则可以不需要事务
        if (txn == null)
            throw new RuntimeException("Must enlist transaction");
        //TODO:判断模型运行时及持久化状态
        var model = entity.model(); //肯定存在，不需要RuntimeContext.Current.GetEntityModel
        //暂不允许没有成员的插入操作
        //if (model.Members.Count == 0)
        //    throw new RuntimeException("Entity[{model.Name}] has no member");

        var                     app = RuntimeContext.current().getApplicationModel(model.appId());
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
            //TODO:插入索引，注意变更后可能已添加或删除了索引会报错

            //插入数据
            var req = new KVInsertEntityRequest(entity, model, txn.id()); //TODO: refs
            req.raftGroupId      = raftGroupId;
            req.schemaVersion    = model.sysStoreOptions().schemaVersion();
            req.overrideIfExists = false;
            return SysStoreApi.execKVInsertAsync(req);
        }).thenAccept(res -> {
            if (res.errorCode != 0) {
                throw new SysStoreException(res.errorCode);
            }
        });
    }
    //endregion

}
