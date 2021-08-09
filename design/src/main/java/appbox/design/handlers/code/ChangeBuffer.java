package appbox.design.handlers.code;

import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.design.tree.ModelNode;
import appbox.model.ModelType;
import appbox.model.ViewModel;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

/**
 * 前端改变了模型或表达式的代码，后端进行同步与reparse
 */
public final class ChangeBuffer implements IDesignHandler { //TODO: rename
    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var type     = args.getInt();
        var targetId = args.getString();

        var modelId   = Long.parseUnsignedLong(targetId);
        var modelNode = hub.designTree.findModelNode(modelId);
        if (modelNode == null) {
            var error = String.format("Can't find model: %d", modelId);
            return CompletableFuture.failedFuture(new RuntimeException(error));
        }

        if (modelNode.model().modelType() == ModelType.Service) {
            return changeJavaCode(hub, args, type, modelNode);
        } else if (modelNode.model().modelType() == ModelType.View) {
            return changeDartCode(hub, args, modelNode);
        }
        return CompletableFuture.failedFuture(new RuntimeException("Not supported."));
    }

    /**
     * 改变服务模型的代码(Java)
     */
    private static CompletableFuture<Object> changeJavaCode(DesignHub hub, InvokeArgs args, int type, ModelNode modelNode) {
        var doc = hub.typeSystem.javaLanguageServer.findOpenedDocument(modelNode.model().id());
        if (doc == null) {
            var error = String.format("Can't find opened ServiceModel: %s", modelNode.model().name());
            return CompletableFuture.failedFuture(new RuntimeException(error));
        }

        //注意队列顺序执行
        if (type >> 16 == 1) { //Changed by offset
            var offset  = args.getInt();
            var length  = args.getInt();
            var newText = args.getString();
            CompletableFuture.runAsync(() -> {
                hub.typeSystem.javaLanguageServer.changeDocument(doc, offset, length, newText);
                //Log.debug(doc.getText());
            }, hub.codeEditorTaskPool);
        } else { //Changed by position
            var startLine   = args.getInt() - 1; //注意前端值-1
            var startColumn = args.getInt() - 1;
            var endLine     = args.getInt() - 1;
            var endColumn   = args.getInt() - 1;
            var newText     = args.getString();
            //Log.debug(String.format("%d %s %d.%d-%d.%d %s", type, targetId, startLine, startColumn, endLine, endColumn, newText));
            CompletableFuture.runAsync(() -> {
                hub.typeSystem.javaLanguageServer.changeDocument(doc, startLine, startColumn, endLine, endColumn, newText);
                //Log.debug(doc.getText());
            }, hub.codeEditorTaskPool);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 改变视图模型的代码(Dart)
     */
    private static CompletableFuture<Object> changeDartCode(DesignHub hub, InvokeArgs args, ModelNode modelNode) {
        final var model = (ViewModel) modelNode.model();
        if (model.getType() != ViewModel.TYPE_FLUTTER)
            return CompletableFuture.failedFuture(new RuntimeException("Not supported."));

        var offset  = args.getInt();
        var length  = args.getInt();
        var newText = args.getString();
        CompletableFuture.runAsync(() -> {
            hub.typeSystem.dartLanguageServer.changeDocument(modelNode, offset, length, newText);
        }, hub.codeEditorTaskPool);

        return CompletableFuture.completedFuture(null);
    }
}
