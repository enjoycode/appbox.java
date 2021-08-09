package appbox.design.handlers.service;

import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.model.ModelType;
import appbox.model.ServiceModel;
import appbox.runtime.InvokeArgs;
import appbox.store.utils.AssemblyUtil;

import java.util.concurrent.CompletableFuture;

public final class OpenServiceModel implements IDesignHandler {
    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        final var modelId   = Long.parseUnsignedLong(args.getString());
        final var modelNode = hub.designTree.findModelNode(ModelType.Service, modelId);
        if (modelNode == null) {
            return CompletableFuture.failedFuture(new Exception("Can't find service model"));
        }
        final var appName = modelNode.appNode.model.name();
        final var model   = (ServiceModel) modelNode.model();

        //先释放第三方引用(可能集群节点切换或其他原因导致jar文件已不存在)
        return AssemblyUtil.extractService3rdLibs(appName, model.getReferences())
                .thenApply(r -> {
                    //注意与C#实现不同，不需要从Staged或存储加载代码，由虚拟文件加载代码
                    try {
                        final var doc = hub.typeSystem.javaLanguageServer.openDocument(modelNode);
                        return doc.getContents();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
