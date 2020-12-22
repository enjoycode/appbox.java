package appbox.design.handlers;

import appbox.design.DesignHub;
import appbox.design.tree.DesignNodeType;
import appbox.design.tree.ModelNode;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

/** 保存当前的模型，注意：某些模型需要传入附加的参数 */
public final class SaveModel implements IDesignHandler {
    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var nodeType = DesignNodeType.fromValue((byte) args.getInt());
        var modelId  = args.getString();

        Object[] modelInfo = null;
        if (nodeType == DesignNodeType.ViewModelNode) {
            modelInfo    = new Object[4];
            modelInfo[0] = args.getString();
            modelInfo[1] = args.getString();
            modelInfo[2] = args.getString();
            modelInfo[3] = args.getString();
        } else if (nodeType == DesignNodeType.ReportModelNode) {
            modelInfo    = new Object[1];
            modelInfo[0] = args.getString();
        }

        var node = hub.designTree.findNode(nodeType, modelId);
        if (node == null)
            return CompletableFuture.failedFuture(new RuntimeException("Can't find node"));

        var modelNode = (ModelNode) node;
        return modelNode.saveAsync(modelInfo).thenApply(r -> null);
    }
}
