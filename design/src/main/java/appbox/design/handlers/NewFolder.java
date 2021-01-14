package appbox.design.handlers;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.design.common.NewNodeResult;
import appbox.design.tree.DesignNodeType;
import appbox.design.tree.DesignTree;
import appbox.design.tree.FolderNode;
import appbox.design.tree.ModelRootNode;
import appbox.model.ModelFolder;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

public final class NewFolder implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var selectedNodeType = DesignNodeType.fromValue((byte) args.getInt());
        var selectedNodeId   = args.getString();
        var name             = args.getString();
        if (name == null || name.isEmpty())
            throw new IllegalArgumentException("Name is null");

        var selectedNode = hub.designTree.findNode(selectedNodeType, selectedNodeId);
        if (selectedNode == null)
            throw new RuntimeException("Can't find node");
        //根据选择的节点获取合适的插入位置
        var parentNode = DesignTree.findNewFolderParentNode(selectedNode);
        if (parentNode == null)
            throw new RuntimeException("Can't find parent node");
        //判断有无同名文件夹存在
        if (parentNode.nodes.exists(n -> n.nodeType() == DesignNodeType.FolderNode && n.text().equals(name)))
            throw new RuntimeException("Name has exists");

        //判断当前模型根节点有没有签出
        ModelRootNode rootNode = null;
        if (parentNode.nodeType() == DesignNodeType.FolderNode) {
            var parentFolder = ((FolderNode) parentNode).folder;
            rootNode = hub.designTree.findModelRootNode(parentFolder.appId(), parentFolder.targetModelType());
        } else {
            rootNode = (ModelRootNode) parentNode;
        }
        boolean       rootNodeHasCheckout = rootNode.isCheckoutByMe();
        ModelRootNode finalRootNode       = rootNode;
        return rootNode.checkout().thenCompose(checkoutOK -> {
            if (!checkoutOK) {
                throw new RuntimeException(String.format("Can't checkout: %s", finalRootNode.fullName()));
            }

            //根据上级节点是ModelRootNode or FolderNode创建ModelFolder
            ModelFolder folder = null;
            if (parentNode.nodeType() == DesignNodeType.FolderNode) {
                folder = new ModelFolder(((FolderNode) parentNode).folder, name);
            } else {
                folder = new ModelFolder(finalRootNode.rootFolder(), name);
            }

            var node = new FolderNode(folder);
            //添加至设计树
            var insertIndex = parentNode.nodes.add(node);
            //添加至根节点索引内
            finalRootNode.addFolderIndex(node);
            //保存至Staged
            return node.saveAsync().thenApply(re -> new NewNodeResult(
                    parentNode.nodeType().value, parentNode.id(), node
                    , rootNodeHasCheckout ? null : finalRootNode.id(), insertIndex));
        }).thenApply(JsonResult::new);
    }

}
