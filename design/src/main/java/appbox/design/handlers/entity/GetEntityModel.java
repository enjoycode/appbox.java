package appbox.design.handlers.entity;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.design.handlers.IRequestHandler;
import appbox.design.tree.DataStoreNode;
import appbox.design.tree.DesignNodeType;
import appbox.logging.Log;
import appbox.model.EntityModel;
import appbox.runtime.InvokeArg;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GetEntityModel implements IRequestHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, List<InvokeArg> args) {
        var modelId   = Long.parseUnsignedLong(args.get(0).getString());
        var modelNode = hub.designTree.findModelNode(modelId);
        if (modelNode == null) {
            var error = String.format("Cannot find EntityModel: %s", modelId);
            return CompletableFuture.failedFuture(new Exception(error));
        }
        var model = (EntityModel) modelNode.model();
        if (model.sysStoreOptions() != null) {
            //TODO 加载变更添加的索引的构建状态
        } else if (model.sqlStoreOptions() != null) {
            var storeNode = (DataStoreNode) hub.designTree.findNode(DesignNodeType.DataStoreNode
                    , Long.toUnsignedString(model.sqlStoreOptions().storeModelId()));
            if (storeNode == null)
                Log.warn("Can't find DataStoreNode");
            model.sqlStoreOptions().setDataStoreModel(storeNode.model()); //set cache
        } //TODO:CqlStore同上

        return CompletableFuture.completedFuture(new JsonResult(modelNode.model()));
    }
}
