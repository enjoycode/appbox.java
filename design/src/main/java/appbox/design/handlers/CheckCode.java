package appbox.design.handlers;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.model.ModelType;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

public final class CheckCode implements IDesignHandler {
    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        int type     = args.getInt();
        var targetId = args.getString();
        //Log.debug(String.format("CheckCode: %d %s", type, targetId));

        var modelId   = Long.parseUnsignedLong(targetId);
        var modelNode = hub.designTree.findModelNode(ModelType.Service, modelId);
        if (modelNode == null) {
            var error = String.format("Can't find ServiceModel: %d", modelId);
            return CompletableFuture.failedFuture(new Exception(error));
        }
        var doc       = hub.typeSystem.languageServer.findOpenedDocument(modelId);
        if (doc == null) {
            var error = String.format("Can't find ServiceModel: %d", modelId);
            return CompletableFuture.failedFuture(new Exception(error));
        }
        return CompletableFuture.supplyAsync(() -> {
            var problems = hub.typeSystem.languageServer.diagnostics(doc);
            return new JsonResult(problems);
        }, hub.codeEditorTaskPool);
    }

}
