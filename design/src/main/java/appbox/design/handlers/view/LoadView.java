package appbox.design.handlers.view;

import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.design.services.StagedService;
import appbox.model.ModelType;
import appbox.runtime.InvokeArgs;
import appbox.store.ModelStore;
import appbox.store.utils.ModelCodeUtil;

import java.util.concurrent.CompletableFuture;

public final class LoadView implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var modelId = args.getString(); //eg: sys.CustomerListView
        var sr      = modelId.split("\\.");
        var app     = hub.designTree.findApplicationNodeByName(sr[0]);
        var node    = hub.designTree.findModelNodeByName(app.model.id(), ModelType.View, sr[1]);
        if (node == null)
            throw new RuntimeException("Can't find view node: " + modelId);

        //已签出先尝试从Staged中加载
        CompletableFuture<String> task = node.isCheckoutByMe() ?
                StagedService.loadViewRuntimeCode(node.model().id()) : CompletableFuture.completedFuture(null);
        return task.thenCompose(code -> {
            if (code != null)
                return CompletableFuture.completedFuture(code);
            return ModelStore.loadViewAssemblyAsync(modelId)
                    .thenApply(ModelCodeUtil::decodeViewRuntimeCode);
        }).thenApply(r -> r);
    }

}
