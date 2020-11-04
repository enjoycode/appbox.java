package appbox.store;

import appbox.channel.messages.KVGetPartitionRequest;
import appbox.model.ApplicationModel;
import appbox.model.EntityModel;
import appbox.store.caching.MetaCaches;

import java.util.concurrent.CompletableFuture;

public class EntityStore { //TODO: rename to SysStore

    //region ====分区信息====
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

}
