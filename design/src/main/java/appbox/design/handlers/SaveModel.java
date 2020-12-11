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
        var nodeType = DesignNodeType.forValue((byte) args.getInt());
        var modelId  = args.getString();

        if (nodeType == DesignNodeType.ViewModelNode) {
            throw new RuntimeException("未实现");
        } else if (nodeType == DesignNodeType.ReportModelNode) {
            throw new RuntimeException("未实现");
        }

        var node = hub.designTree.findNode(nodeType, modelId);
        if (node == null)
            return CompletableFuture.failedFuture(new RuntimeException("Can't find node"));

        var modelNode = (ModelNode) node;
        return modelNode.saveAsync(null).thenApply(r -> null);
    }
}
