package appbox.store.sysquery;

import appbox.channel.messages.KVScanTableRequest;
import appbox.channel.messages.KVScanTableResponse;
import appbox.data.SysEntity;
import appbox.expressions.Expression;
import appbox.model.EntityModel;
import appbox.runtime.RuntimeContext;
import appbox.store.EntityStore;
import appbox.store.ReadonlyTransaction;
import appbox.store.SysStoreApi;
import appbox.utils.IdUtil;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class TableScan<T extends SysEntity> extends KVScan {
    private final Class<T> _clazz;

    public TableScan(long modelId, Class<T> clazz) {
        super(modelId);
        _clazz = clazz;
    }

    //region ====where methods====
    public TableScan<T> where(Expression filter) {
        this.filter = filter;
        return this;
    }
    //endregion

    //region ====toXXX methods====
    private CompletableFuture<KVScanTableResponse<T>> execPartScanAsync(long raftGroupId, int skip, int take) {
        if (raftGroupId == 0)
            return CompletableFuture.completedFuture(new KVScanTableResponse<>(null));

        var req = new KVScanTableRequest(raftGroupId, skip, take, filter);
        return SysStoreApi.execKVScanAsync(req, new KVScanTableResponse<>(_clazz));
    }

    public CompletableFuture<List<T>> toListAsync(/*ITransaction txn*/) {
        var         app   = RuntimeContext.current().getApplicationModel(IdUtil.getAppIdFromModelId(modelId));
        EntityModel model = RuntimeContext.current().getModel(modelId);

        //先判断是否需要快照读事务 //TODO:跨分区也需要
        //ReadonlyTransaction txn = rootIncluder == null ? null : new ReadonlyTransaction();
        ReadonlyTransaction txn = null;

        //根据是否分区执行不同的查询
        if (model.sysStoreOptions().hasPartitionKeys()) {
            throw new RuntimeException("未实现");
        } else {
            return EntityStore.getOrCreateGlobalTablePartition(app, model, txn)
                    .thenCompose(raftGroupId -> execPartScanAsync(raftGroupId, skip, take))
                    .thenApply(res -> res.result); //TODO:处理Includes
        }
    }
    //endregion

}
