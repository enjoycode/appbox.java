package appbox.design.handlers;

import appbox.design.DesignHub;
import appbox.logging.Log;
import appbox.model.ModelType;
import appbox.runtime.InvokeArg;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ChangeBuffer implements IRequestHandler {
    @Override
    public CompletableFuture<Object> handle(DesignHub hub, List<InvokeArg> args) {
        var type        = args.get(0).getInt();
        var targetId    = args.get(1).getString();
        var startLine   = args.get(2).getInt() - 1; //注意前端值-1
        var startColumn = args.get(3).getInt() - 1;
        var endLine     = args.get(4).getInt() - 1;
        var endColumn   = args.get(5).getInt() - 1;
        var newText     = args.get(6).getString();

        Log.debug(String.format("ChangeBuffer: %d %s %d.%d-%d.%d %s",
                type, targetId, startLine, startColumn, endLine, endColumn, newText));

        var modelId = Long.parseUnsignedLong(targetId);
        var modelNode = hub.designTree.findModelNode(ModelType.Service, modelId);
        if (modelNode == null) {
            var error = String.format("Can't find ServiceModel: %d", modelId);
            return CompletableFuture.failedFuture(new Exception(error));
        }

        var fileName = String.format("%s.Services.%s.java",
                modelNode.appNode.model.name(), modelNode.model().name());
        var doc = hub.typeSystem.opendDocs.get(fileName);
        if (doc == null) {
            var error = String.format("Can't find opened ServiceModel: %s", fileName);
            return CompletableFuture.failedFuture(new Exception(error));
        }

        doc.sourceText.changeText(startLine, startColumn, endLine, endColumn, newText);
        //Log.debug(doc.sourceText.toString());

        return CompletableFuture.completedFuture(null);
    }
}
