package appbox.design.handlers;

import appbox.design.DesignHub;
import appbox.design.tree.DesignNodeType;
import appbox.design.tree.ModelNode;
import appbox.logging.Log;
import appbox.runtime.InvokeArg;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class Checkout implements IRequestHandler {
    /**
     * 签出编辑模型
     * @return true表示签出时模型已被其他人改变
     */
    @Override
    public CompletableFuture<Object> handle(DesignHub hub, List<InvokeArg> args) {
        var nodeType = DesignNodeType.forValue((byte) args.get(0).getInt());
        var nodeId   = args.get(1).getString();

        var node = hub.designTree.findNode(nodeType, nodeId);
        if (node == null)
            return CompletableFuture.failedFuture(new RuntimeException("Can't find node"));

        if (node instanceof ModelNode) {
            var modelNode = (ModelNode) node;
            var curVersion = modelNode.model().version();
            return modelNode.checkout().thenApply(checkoutOK -> {
                if (!checkoutOK)
                    throw new RuntimeException("Can't checkout ModelNode:" + modelNode.model().name());
                return curVersion != modelNode.model().version(); //返回True表示模型已变更，用于前端刷新
            });
        } else {
            Log.warn("暂未实现签出其他类型节点");
            return CompletableFuture.completedFuture(false);
        }
    }
}
