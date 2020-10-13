package appbox.design.handlers.service;

import appbox.design.DesignHub;
import appbox.design.handlers.IRequestHandler;
import appbox.model.ModelType;
import appbox.runtime.InvokeArg;

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

        //注意与C#实现不同，不需要从Staged或存储加载代码，由虚拟文件加载代码
        var doc = hub.typeSystem.workspace.openDocument(modelId);
        return CompletableFuture.completedFuture(doc.getText()/*doc.getCharsSequence()*/);
    }
}
