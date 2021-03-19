package appbox.design.handlers.tree;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

public final class LoadDesignTree implements IDesignHandler {
    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        return hub.designTree.loadNodesAsync().thenApply(r -> new JsonResult(hub.designTree.nodes));
    }
}
