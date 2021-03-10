package appbox.design.handlers.store;

import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.design.tree.DataStoreNode;
import appbox.design.tree.DesignNodeType;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

public final class SaveDataStore implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        //TODO:检查是否签出

        var nodeId   = args.getString();
        var settings = args.getString();

        var node = hub.designTree.findNode(DesignNodeType.DataStoreNode, nodeId);
        if (node == null)
            return CompletableFuture.failedFuture(new RuntimeException("Can't find DataStore"));

        var dataStoreNode = (DataStoreNode) node;
        dataStoreNode.model().updateSettings(settings);
        return dataStoreNode.saveAsync(false)
                .thenApply(r -> null);
    }

}
