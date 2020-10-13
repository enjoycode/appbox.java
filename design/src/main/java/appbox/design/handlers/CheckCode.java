package appbox.design.handlers;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.logging.Log;
import appbox.model.ModelType;
import appbox.runtime.InvokeArg;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class CheckCode implements IRequestHandler {
    @Override
    public CompletableFuture<Object> handle(DesignHub hub, List<InvokeArg> args) {
        int type     = args.get(0).getInt();
        var targetId = args.get(1).getString();
        Log.debug(String.format("CheckCode: %d %s", type, targetId));
        var modelId   = Long.parseUnsignedLong(targetId);
        var modelNode = hub.designTree.findModelNode(ModelType.Service, modelId);
        if (modelNode == null) {
            var error = String.format("Can't find ServiceModel: %d", modelId);
            return CompletableFuture.failedFuture(new Exception(error));
        }

        return CompletableFuture.completedFuture(new JsonResult(null));
        //var fileName = String.format("%s.Services.%s.java",
        //        modelNode.appNode.model.name(), modelNode.model().name());
        //var doc = hub.typeSystem.openedDocs.get(fileName);
        //if (doc == null) {
        //    var error = String.format("Can't find opened ServiceModel: %s", fileName);
        //    return CompletableFuture.failedFuture(new Exception(error));
        //}
        //return CompletableFuture.supplyAsync(() -> {
        //          return new JsonResult(null);
        //    //}
        //}, hub.codeEditorTaskPool);
    }

}
