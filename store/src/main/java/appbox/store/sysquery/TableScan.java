package appbox.store.sysquery;

import appbox.data.Entity;
import appbox.data.SysEntity;
import appbox.model.EntityModel;
import appbox.runtime.RuntimeContext;
import appbox.store.ReadonlyTransaction;
import appbox.utils.IdUtil;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class TableScan<T extends SysEntity> extends KVScan {
    public TableScan(long modelId) {
        super(modelId);
    }

    //region ====toXXX methods====
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

        }
        throw new RuntimeException("未实现");
    }
    //endregion

}
