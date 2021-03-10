package appbox.design.handlers.store;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.model.DataStoreModel;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

public final class NewDataStore implements IDesignHandler {
    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var kind     = DataStoreModel.DataStoreKind.fromValue((byte) args.getInt());
        var provider = args.getString();
        var settings = args.getString();

        //TODO:验证名称有效性及是否存在

        //新建存储节点
        var model = new DataStoreModel(kind, provider, settings);
        //添加节点至模型树并绑定签出信息
        var node = hub.designTree.storeRootNode().addModel(model, hub, true);

        return node.saveAsync(true).thenApply(r -> {
            hub.typeSystem.updateStoresDocument();
            return new JsonResult(node);
        });
    }
}
