package appbox.design.handlers.service;

import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.model.ModelType;
import appbox.model.ServiceModel;
import appbox.runtime.InvokeArgs;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** 用于IDE更新服务模型的第三方包引用 */
public final class UpdateReferences implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        final var modelId = Long.parseUnsignedLong(args.getString());
        final var newDeps = args.getStringArray();

        final var modelNode = hub.designTree.findModelNode(ModelType.Service, modelId);
        if (modelNode == null)
            return CompletableFuture.failedFuture(new RuntimeException("Can't find service node"));
        final var model = (ServiceModel) modelNode.model();

        //先更新服务模型的第三方依赖
        model.setReferences(List.of(newDeps));
        //再通知类型系统更新
        return hub.typeSystem.javaLanguageServer.updateServiceReferences(modelNode).thenApply(r -> null);
    }

}
