package appbox.design.handlers;

import appbox.design.DesignHub;
import appbox.design.tree.DesignNodeType;
import appbox.runtime.InvokeArg;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class CloseDesigner implements IRequestHandler {
    @Override
    public CompletableFuture<Object> handle(DesignHub hub, List<InvokeArg> args) {
        var nodeType = DesignNodeType.forValue((byte) args.get(0).getInt());
        var modelId   = Long.parseUnsignedLong(args.get(1).getString());

        if (nodeType == DesignNodeType.ServiceModelNode)
        {
            //注意可能已被删除了，即由删除节点引发的关闭
            hub.typeSystem.languageServer.closeDocument(modelId);
        }


        return CompletableFuture.completedFuture(null);
    }
}
