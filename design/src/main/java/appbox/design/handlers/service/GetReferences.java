package appbox.design.handlers.service;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.model.ModelType;
import appbox.model.ServiceModel;
import appbox.runtime.InvokeArgs;
import appbox.serialization.IJsonSerializable;
import appbox.serialization.IJsonWriter;
import appbox.store.ModelStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/** 获取当前服务模型的依赖项,同时返回所属App所依赖的第三方库列表 */
public final class GetReferences implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        final var modelId   = Long.parseUnsignedLong(args.getString());
        final var modelNode = hub.designTree.findModelNode(ModelType.Service, modelId);
        if (modelNode == null)
            return CompletableFuture.failedFuture(new RuntimeException("Can't find service model"));
        final var model = (ServiceModel) modelNode.model();
        final var appName = modelNode.appNode.model.name();

        return ModelStore.loadAppAssemblies(appName)
                .thenApply(names -> {
                    final var appDeps = Arrays.stream(names)
                            .map(n -> n.substring(appName.length() + 1))
                            .collect(Collectors.toList());
                    final var modelDeps = model.getReferences();
                    return new JsonResult(new Dependencies(appDeps, modelDeps));
                });
    }

    static final class Dependencies implements IJsonSerializable {
        public final List<String> appDeps;
        public final List<String> modelDeps;

        public Dependencies(List<String> appDeps, List<String> modelDeps) {
            this.appDeps   = appDeps;
            this.modelDeps = modelDeps;
        }

        @Override
        public void writeToJson(IJsonWriter writer) {
            writer.startObject();
            writer.writeKeyValue("AppDeps", appDeps);
            writer.writeKeyValue("ModelDeps", modelDeps);
            writer.endObject();
        }

    }

}
