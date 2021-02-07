package appbox.design.handlers;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.design.common.NewNodeResult;
import appbox.design.tree.ApplicationNode;
import appbox.model.ApplicationModel;
import appbox.runtime.InvokeArgs;
import appbox.store.ModelStore;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/** 创建新的应用 */
public final class NewApplication implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var appName = args.getString();

        var node = hub.designTree.findApplicationNodeByName(appName);
        if (node != null)
            throw new RuntimeException("Application has exists");
        if (appName.getBytes(StandardCharsets.UTF_8).length > 8)
            throw new RuntimeException("Application name too long");
        var appRootNode = hub.designTree.appRootNode();
        var appModel    = new ApplicationModel("appbox", appName); //TODO: fix owner
        var appNode     = new ApplicationNode(hub.designTree, appModel);
        appRootNode.nodes.add(appNode);

        return ModelStore.createApplicationAsync(appModel).thenApply(r -> new JsonResult(
                new NewNodeResult(
                        appRootNode.nodeType().value,
                        appRootNode.id(),
                        appNode,
                        null,
                        0))); //TODO: fix insert index
    }

}
