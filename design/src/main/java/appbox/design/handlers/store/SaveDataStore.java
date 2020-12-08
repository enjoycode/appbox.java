package appbox.design.handlers.store;

import appbox.design.DesignHub;
import appbox.design.handlers.IRequestHandler;
import appbox.design.tree.DataStoreNode;
import appbox.design.tree.DesignNodeType;
import appbox.runtime.InvokeArg;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class SaveDataStore implements IRequestHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, List<InvokeArg> args) {
        //TODO:检查是否签出

        var nodeId   = args.get(0).getString();
        var settings = args.get(1).getString();

        var node = hub.designTree.findNode(DesignNodeType.DataStoreNode, nodeId);
        if (node == null)
            return CompletableFuture.failedFuture(new RuntimeException("Can't find DataStore"));

        var dataStoreNode = (DataStoreNode) node;
        dataStoreNode.model().updateSettings(settings);
        return dataStoreNode.saveAsync().thenApply(r -> null);
    }

}
