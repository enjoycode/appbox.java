package appbox.design.handlers.store;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.model.DataStoreModel;
import appbox.runtime.InvokeArgs;
import appbox.store.ModelStore;

import java.util.concurrent.CompletableFuture;

public final class NewDataStore implements IDesignHandler {
    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var kind     = DataStoreModel.DataStoreKind.fromValue((byte) args.getInt());
        var provider = args.getString();
        var storeName = args.getString();

        //TODO: 验证DataStoreRootNode是否签出及名称有效性及是否存在

        //新建存储节点
        var model = new DataStoreModel(kind, provider, storeName);
        //添加节点至模型树并绑定签出信息
        var node = hub.designTree.storeRootNode().addModel(model, hub, true);

        //系统内置的BlobStore特殊处理
        if (model.isSystemBlobStore()) {
            return ModelStore.createBlobStoreAsync(storeName).thenApply(r -> new JsonResult(node));
        }

        //以下第三方存储的处理
        return node.saveAsync(true).thenApply(r -> {
            hub.typeSystem.javaLanguageServer.filesManager.updateStoresDocument();
            return new JsonResult(node);
        });
    }
}
