package appbox.design.handlers;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.runtime.InvokeArg;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class LoadDesignTree implements IRequestHandler {
    @Override
    public CompletableFuture<Object> handle(DesignHub hub, List<InvokeArg> args) {
        return hub.designTree.loadNodesAsync().thenApply(r -> {
            return new JsonResult(hub.designTree.nodes.nodes);
        });
    }
}
