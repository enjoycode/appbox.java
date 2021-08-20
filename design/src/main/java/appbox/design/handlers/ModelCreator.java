package appbox.design.handlers;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.design.common.CheckoutInfo;
import appbox.design.common.NewNodeResult;
import appbox.design.tree.DesignNodeType;
import appbox.design.tree.DesignTree;
import appbox.design.tree.FolderNode;
import appbox.design.tree.ModelNode;
import appbox.design.utils.CodeHelper;
import appbox.model.*;
import appbox.store.ModelStore;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/** 用于新建模型及相应的节点 */
public final class ModelCreator {
    private ModelCreator() {}

    public static CompletableFuture<Object> create(DesignHub hub, ModelType modelType
            , Function<Long, ? extends ModelBase> creator
            , DesignNodeType selectedNodeType, String selectedNodeId, String name, Object[] initCodes) {
        // 验证名称有效性
        if (name == null || name.isEmpty() || !CodeHelper.isValidIdentifier(name))
            throw new RuntimeException("Name invalid");
        // 获取选择的节点
        var selectedNode = hub.designTree.findNode(selectedNodeType, selectedNodeId);
        if (selectedNode == null)
            throw new RuntimeException("Can't find node");
        //根据选择的节点获取合适的插入位置
        var parentNode = DesignTree.findNewModelParentNode(selectedNode, modelType);
        if (parentNode == null)
            throw new RuntimeException("Can't find parent node");
        var appNode = DesignTree.findAppNodeFromNode(parentNode);
        //判断名称是否已存在
        if (hub.designTree.findModelNodeByName(appNode.model.id(), modelType, name) != null)
            throw new RuntimeException("Name has exists");

        //判断当前模型根节点有没有签出
        var rootNode            = hub.designTree.findModelRootNode(appNode.model.id(), modelType);
        var rootNodeHasCheckout = rootNode.isCheckoutByMe();
        return rootNode.checkout().thenCompose(checkoutOK -> {
            if (!checkoutOK)
                throw new RuntimeException(String.format("Can't checkout: %s", rootNode.fullName()));

            //生成模型标识号并新建模型及节点 //TODO:fix Layer
            return ModelStore.genModelIdAsync(appNode.model.id(), modelType, ModelLayer.DEV);
        }).thenCompose(modelId -> {
            var model       = creator.apply(modelId);
            var node        = new ModelNode(model, hub);
            var insertIndex = parentNode.nodes.add(node);
            //设置文件夹
            if (parentNode.nodeType() == DesignNodeType.FolderNode)
                model.setFolderId(((FolderNode) parentNode).folder.id());
            // 添加至根节点索引内
            rootNode.addModelIndex(node);

            //设为签出状态
            node.setCheckoutInfo(new CheckoutInfo(node.nodeType(), node.checkoutInfoTargetID(), model.version(),
                    hub.session.name(), hub.session.leafOrgUnitId()));

            //保存至Staged
            return node.saveAsync(initCodes).thenApply(re -> {
                //创建虚拟文件
                hub.typeSystem.createModelDocument(node, false);

                return new NewNodeResult(parentNode.nodeType().value, parentNode.id(), node
                        , rootNodeHasCheckout ? null : rootNode.id(), insertIndex);
            });
        }).thenApply(JsonResult::new);
    }

}
