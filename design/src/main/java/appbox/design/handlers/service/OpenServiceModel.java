package appbox.design.handlers.service;

import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.model.ModelType;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

public final class OpenServiceModel implements IDesignHandler {
    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var modelId   = Long.parseUnsignedLong(args.getString());
        var modelNode = hub.designTree.findModelNode(ModelType.Service, modelId);
        if (modelNode == null) {
            var error = new Exception("Can't find service model");
            return CompletableFuture.failedFuture(error);
        }

        //注意与C#实现不同，不需要从Staged或存储加载代码，由虚拟文件加载代码
        try {
            var doc = hub.typeSystem.languageServer.openDocument(modelNode);
            return CompletableFuture.completedFuture(doc.getContents());
        } catch (Exception ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }
}
