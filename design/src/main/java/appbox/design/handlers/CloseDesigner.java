package appbox.design.handlers;

import appbox.design.DesignHub;
import appbox.design.tree.DesignNodeType;
import appbox.model.ViewModel;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

public final class CloseDesigner implements IDesignHandler {
    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var nodeType = DesignNodeType.fromValue((byte) args.getInt());
        var modelId  = Long.parseUnsignedLong(args.getString());

        //注意可能已被删除了，即由删除节点引发的关闭
        if (nodeType == DesignNodeType.ServiceModelNode) {
            hub.typeSystem.languageServer.closeDocument(modelId);
        } else if (nodeType == DesignNodeType.ViewModelNode) {
            //TODO:如果由删除节点激发的,则前端必须先调用此关闭后再删除
            var modelNode = hub.designTree.findModelNode(modelId);
            if (modelNode == null) {
                var error = String.format("Can't find model: %d", modelId);
                return CompletableFuture.failedFuture(new RuntimeException(error));
            }
            final var model = (ViewModel) modelNode.model();
            if (model.getType() == ViewModel.TYPE_VUE) {
                hub.dartLanguageServer.closeDocument(modelNode);
            }
        }

        return CompletableFuture.completedFuture(null);
    }
}
