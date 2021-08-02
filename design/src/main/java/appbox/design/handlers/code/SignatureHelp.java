package appbox.design.handlers.code;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.model.ModelType;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

public final class SignatureHelp implements IDesignHandler {

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
            final var doc     = hub.typeSystem.languageServer.findOpenedDocument(modelId);
            if (doc == null) {
                var error = String.format("Can't find opened ServiceModel: %s", fileName);
                return CompletableFuture.failedFuture(new RuntimeException(error));
            }

            //暂在同一线程内处理
            return CompletableFuture.supplyAsync(() -> {
                final var res = hub.typeSystem.languageServer.signatureHelp(doc, line, column);
                return new JsonResult(res);
            }, hub.codeEditorTaskPool);
        } else if (modelNode.model().modelType() == ModelType.View) {
            return CompletableFuture.failedFuture(new RuntimeException("未实现"));
        }

        return CompletableFuture.failedFuture(new RuntimeException("Not supported"));
    }

}
