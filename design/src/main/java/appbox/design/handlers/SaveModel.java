package appbox.design.handlers;

import appbox.design.DesignHub;
import appbox.design.tree.DesignNodeType;
import appbox.design.tree.ModelNode;
import appbox.runtime.InvokeArg;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** 保存当前的模型，注意：某些模型需要传入附加的参数 */
public final class SaveModel implements IRequestHandler {
    @Override
    public CompletableFuture<Object> handle(DesignHub hub, List<InvokeArg> args) {
        var nodeType = DesignNodeType.forValue((byte) args.get(0).getInt());
        var modelId  = args.get(1).getString();

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
