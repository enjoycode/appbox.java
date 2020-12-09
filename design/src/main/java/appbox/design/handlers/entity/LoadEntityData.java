package appbox.design.handlers.entity;

import appbox.data.JsonResult;
import appbox.data.PersistentState;
import appbox.data.SysEntityKVO;
import appbox.design.DesignHub;
import appbox.design.handlers.IRequestHandler;
import appbox.model.EntityModel;
import appbox.model.ModelType;
import appbox.runtime.InvokeArg;
import appbox.store.query.TableScan;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** 用于前端实体模型设计器数据视图面板加载数据 */
public final class LoadEntityData implements IRequestHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, List<InvokeArg> args) {
        var modelId = Long.parseUnsignedLong(args.get(0).getString());
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
        }

        return CompletableFuture.completedFuture(null);
    }

}