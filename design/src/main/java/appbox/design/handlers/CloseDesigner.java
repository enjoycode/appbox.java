package appbox.design.handlers;

import appbox.design.DesignHub;
import appbox.design.tree.DesignNodeType;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

public final class CloseDesigner implements IDesignHandler {
    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var nodeType = DesignNodeType.forValue((byte) args.getInt());
        var modelId  = Long.parseUnsignedLong(args.getString());

        if (nodeType == DesignNodeType.ServiceModelNode) {
            //注意可能已被删除了，即由删除节点引发的关闭
            hub.typeSystem.languageServer.closeDocument(modelId);
        }

        return CompletableFuture.completedFuture(null);
    }
}
