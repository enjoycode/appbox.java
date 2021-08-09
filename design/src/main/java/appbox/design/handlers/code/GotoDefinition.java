package appbox.design.handlers.code;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.model.ModelType;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

public final class GotoDefinition implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        final var fileName = args.getString(); //TODO:考虑修改前端传模型标识
        final var line     = args.getInt() - 1; //前端值-1
        final var column   = args.getInt() - 1;

        var modelNode = hub.designTree.findModelNodeByFileName(fileName);
        if (modelNode == null)
            return CompletableFuture.failedFuture(new RuntimeException("Can't find model: " + fileName));

        if (modelNode.model().modelType() == ModelType.Service) {
            final var modelId = modelNode.model().id();
            final var doc     = hub.typeSystem.javaLanguageServer.findOpenedDocument(modelId);
            if (doc == null) {
                var error = String.format("Can't find opened ServiceModel: %s", fileName);
                return CompletableFuture.failedFuture(new RuntimeException(error));
            }

            final var locations = hub.typeSystem.javaLanguageServer.definition(doc, line, column);
            return CompletableFuture.completedFuture(new JsonResult(locations));
        } else if (modelNode.model().modelType() == ModelType.View) {
            return CompletableFuture.failedFuture(new RuntimeException("未实现"));
        }

        return CompletableFuture.failedFuture(new RuntimeException("Not supported"));
    }

}
