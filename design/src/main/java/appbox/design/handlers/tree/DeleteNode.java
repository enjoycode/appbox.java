package appbox.design.handlers.tree;

import appbox.data.PersistentState;
import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.design.services.StagedService;
import appbox.design.tree.ApplicationNode;
import appbox.design.tree.DesignNode;
import appbox.design.tree.DesignNodeType;
import appbox.design.tree.ModelNode;
import appbox.model.ModelLayer;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

public final class DeleteNode implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var selectedNodeType = DesignNodeType.fromValue((byte) args.getInt());
        var selectedNodeId   = args.getString();
        var deleteNode       = hub.designTree.findNode(selectedNodeType, selectedNodeId);
        if (deleteNode == null)
            throw new RuntimeException("Delete target not exists");
        if (!(deleteNode instanceof ModelNode || deleteNode instanceof ApplicationNode
                || deleteNode.nodeType() == DesignNodeType.FolderNode && deleteNode.nodes.size() == 0))
            throw new RuntimeException("Can't delete it");

        CompletableFuture<DesignNode> deleteTask;
        if (deleteNode instanceof ModelNode) {
            deleteTask = deleteModelNodeAsync(hub, (ModelNode) deleteNode);
        } else {
            throw new RuntimeException("未实现");
            //deleteTask = deleteAppNodeAsync()
        }
        return deleteTask.thenApply(rootNode -> {
            //注意：返回rootNode.ID用于前端重新刷新模型根节点
            return rootNode == null ? "" : rootNode.id();
        });
    }

    private static CompletableFuture<DesignNode> deleteModelNodeAsync(DesignHub hub, ModelNode node) {
        //判断当前节点是否隶属系统层
        if (node.model().modelLayer() == ModelLayer.SYS)
            throw new RuntimeException("Can't delete system model");

        final var rootNode            = hub.designTree.findModelRootNode(node.model().appId(), node.model().modelType());
        final var rootNodeHasCheckout = rootNode.isCheckoutByMe();
        //尝试签出模型节点及根节点
        return node.checkout().thenCompose(ok -> {
            if (!ok)
                throw new RuntimeException("Can't checkout node");
            return rootNode.checkout();
        }).thenCompose(ok -> {
            if (!ok)
                throw new RuntimeException("Can't checkout ModelRootNode");
            //注意：如果自动签出了模型根节点，当前选择的节点需要重新指向，因为Node.Checkout()时已重新加载
            final var newNode = rootNodeHasCheckout ? node : rootNode.findModelNode(node.model().id());
            if (newNode == null)
                throw new RuntimeException("Delete target not exists, refresh tree");
            final var model = newNode.model();

            //TODO:查找引用项

            //判断当前模型是否新建的
            if (model.persistentState() == PersistentState.Detached) {
                return StagedService.deleteModelAsync(model.id()).thenApply(r -> newNode);
            } else {
                model.markDeleted();
                return newNode.saveAsync(null).thenApply(r -> newNode);
            }
        }).thenApply(newnode -> {
            //移除对应的节点
            rootNode.removeModel(newnode);
            //删除虚拟文件
            hub.typeSystem.removeModelDocument(newnode);
            return rootNodeHasCheckout ? null : rootNode;
        });
    }


}
