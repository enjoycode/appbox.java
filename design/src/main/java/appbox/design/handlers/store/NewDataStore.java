package appbox.design.handlers.store;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.design.handlers.IRequestHandler;
import appbox.model.DataStoreModel;
import appbox.runtime.InvokeArg;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class NewDataStore implements IRequestHandler {
    @Override
    public CompletableFuture<Object> handle(DesignHub hub, List<InvokeArg> args) {
        var kind     = DataStoreModel.DataStoreKind.fromValue((byte) args.get(0).getInt());
        var provider = args.get(1).getString();
        var settings = args.get(2).getString();

        //TODO:验证名称有效性及是否存在

        //新建存储节点
        var model = new DataStoreModel(kind, provider, settings);
        //添加节点至模型树并绑定签出信息
        var node = hub.designTree.storeRootNode().addModel(model, hub);

        return node.saveAsync().thenApply(r -> {
            hub.typeSystem.updateStoresDocument();
            return new JsonResult(node);
        });
    }
}
