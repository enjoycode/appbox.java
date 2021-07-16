package appbox.design.handlers.tree;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

public final class LoadDesignTree implements IDesignHandler {
    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        //暂在这里设置IDE的版本
        boolean isFlutterIDE = args != null && args.getBool();
        hub.setIDE(isFlutterIDE);

        return hub.designTree.loadNodesAsync().thenApply(r -> new JsonResult(hub.designTree.nodes));
    }
}
