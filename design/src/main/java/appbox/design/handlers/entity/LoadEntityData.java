package appbox.design.handlers.entity;

import appbox.data.JsonResult;
import appbox.data.PersistentState;
import appbox.data.SqlEntityKVO;
import appbox.data.SysEntityKVO;
import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.model.EntityModel;
import appbox.model.ModelType;
import appbox.runtime.InvokeArgs;
import appbox.store.query.SqlQuery;
import appbox.store.query.TableScan;

import java.util.concurrent.CompletableFuture;

/** 用于前端实体模型设计器数据视图面板加载数据 */
public final class LoadEntityData implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var modelId = Long.parseUnsignedLong(args.getString());
        var modelNode = hub.designTree.findModelNode(ModelType.Entity, modelId);
        if (modelNode == null)
            return CompletableFuture.failedFuture(new RuntimeException("Can't find EntityModel"));
        var model = (EntityModel)modelNode.model();
        if (model.persistentState() == PersistentState.Detached) {
            return CompletableFuture.completedFuture(null);
        }

        if (model.sysStoreOptions() != null) {
            var q = new TableScan<>(modelId, SysEntityKVO.class);
            q.take(20);
            return q.toListAsync().thenApply(JsonResult::new);
        } else if (model.sqlStoreOptions() != null) {
            var q = new SqlQuery<>(modelId, SqlEntityKVO.class);
            q.take(20);
            return q.toListAsync().thenApply(JsonResult::new);
        }

        return CompletableFuture.completedFuture(null);
    }

}
