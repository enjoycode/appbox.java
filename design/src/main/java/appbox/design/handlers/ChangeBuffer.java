package appbox.design.handlers;

import appbox.design.DesignHub;
import appbox.model.ModelType;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

/**
 * 前端改变了模型或表达式的代码，后端进行同步与reparse
 */
public final class ChangeBuffer implements IDesignHandler { //TODO: rename
    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var type        = args.getInt();
        var targetId    = args.getString();
        var startLine   = args.getInt() - 1; //注意前端值-1
        var startColumn = args.getInt() - 1;
        var endLine     = args.getInt() - 1;
        var endColumn   = args.getInt() - 1;
        var newText     = args.getString();
        if (newText == null) {
            newText = "";
        }

        //Log.debug(String.format("%d %s %d.%d-%d.%d %s",
        //        type, targetId, startLine, startColumn, endLine, endColumn, newText));

        var modelId   = Long.parseUnsignedLong(targetId);
        var modelNode = hub.designTree.findModelNode(ModelType.Service, modelId);
        if (modelNode == null) {
            var error = String.format("Can't find ServiceModel: %d", modelId);
            return CompletableFuture.failedFuture(new Exception(error));
        }

        var doc = hub.typeSystem.languageServer.findOpenedDocument(modelId);
        if (doc == null) {
            var error = String.format("Can't find opened ServiceModel: %s", modelNode.model().name());
            return CompletableFuture.failedFuture(new Exception(error));
        }

        //注意队列顺序执行
        String finalNewText = newText;
        CompletableFuture.runAsync(() -> {
            hub.typeSystem.languageServer.changeDocument(doc, startLine, startColumn, endLine, endColumn, finalNewText);
            //Log.debug(doc.getText());
        }, hub.codeEditorTaskPool);

        return CompletableFuture.completedFuture(null);
    }
}
