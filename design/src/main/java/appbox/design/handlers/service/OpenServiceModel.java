package appbox.design.handlers.service;

import appbox.design.DesignHub;
import appbox.design.handlers.IRequestHandler;
import appbox.model.ModelType;
import appbox.runtime.InvokeArg;
import appbox.store.ModelStore;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class OpenServiceModel implements IRequestHandler {
    @Override
    public CompletableFuture<Object> handle(DesignHub hub, List<InvokeArg> args) {
        var modelId   = Long.parseUnsignedLong(args.get(0).getString());
        var modelNode = hub.designTree.findModelNode(ModelType.Service, modelId);
        if (modelNode == null) {
            var error = new Exception("Can't find service model");
            return CompletableFuture.failedFuture(error);
        }

        //TODO:暂直接从存储加载源码
        return ModelStore.loadServiceCodeAsync(modelId).thenApply(r -> {
            return r.sourceCode;
        });
    }
}
