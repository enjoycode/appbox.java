package appbox.design.handlers.entity;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.design.common.CheckoutInfo;
import appbox.design.common.NewNodeResult;
import appbox.design.handlers.IDesignHandler;
import appbox.design.tree.DesignNodeType;
import appbox.design.tree.DesignTree;
import appbox.design.tree.ModelNode;
import appbox.design.utils.CodeHelper;
import appbox.model.DataStoreModel;
import appbox.model.EntityModel;
import appbox.model.ModelLayer;
import appbox.model.ModelType;
import appbox.runtime.InvokeArgs;
import appbox.store.ModelStore;

import java.util.concurrent.CompletableFuture;

public class NewEntityModel implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var selectedNodeType = DesignNodeType.fromValue((byte) args.getInt());
        var selectedNodeId   = args.getString();
        var name             = args.getString();
        var localizedName    = args.getString();
        var storeName        = args.getString();
        var orderByDesc      = args.getBool();

        //验证类名称的合法性
        if (name == null || name.isEmpty())
            throw new RuntimeException("Entity name is null");
        if (!CodeHelper.isValidIdentifier(name))
            throw new RuntimeException("Entity name invalid");

        //获取选择的节点
        var selectedNode = hub.designTree.findNode(selectedNodeType, selectedNodeId);
        if (selectedNode == null)
            throw new RuntimeException("Can't find selected node");

        //根据选择的节点获取合适的插入位置
        var parentNode = DesignTree.findNewModelParentNode(selectedNode, ModelType.Entity);
        if (parentNode == null)
            throw new RuntimeException("Can't find parent node");
        var appNode = DesignTree.findAppNodeFromNode(parentNode);

        //判断名称是否已存在
        if (hub.designTree.findModelNodeByName(appNode.model.id(), ModelType.Entity, name) != null)
            throw new RuntimeException("Entity name has exists");

        //判断当前模型根节点有没有签出
        final var rootNode = hub.designTree.findModelRootNode(appNode.model.id(), ModelType.Entity);
        final boolean rootNodeHasCheckout = rootNode.isCheckoutByMe();
        return rootNode.checkout().thenCompose(ok -> {
            if (!ok)
                throw new RuntimeException("Can't checkout model root node");

            //生成模型标识号并新建模型及节点
            return ModelStore.genModelIdAsync(appNode.model.id(), ModelType.Entity, ModelLayer.DEV);
        }).thenCompose(modelId ->  {
            //根据映射的存储创建相应的实体模型
            var entityModel = new EntityModel(modelId, name);
            if (storeName != null && !storeName.isEmpty()) {
                var storeNode = hub.designTree.findDataStoreNodeByName(storeName);
                if (storeNode == null)
                    throw new RuntimeException("Can't find DataStore: " + storeName);
                if (storeNode.model().kind() == DataStoreModel.DataStoreKind.Future) {
                    entityModel.bindToSysStore(true, false); //TODO: fix options
                } else if (storeNode.model().kind() == DataStoreModel.DataStoreKind.Sql) {
                    entityModel.bindToSqlStore(storeNode.model().id());
                    entityModel.sqlStoreOptions().setDataStoreModel(storeNode.model());
                } else {
                    throw new RuntimeException("未实现");
                }
            }

            //TODO:set localizedName

            //添加至树
            final var node = new ModelNode(entityModel, hub);
            final var insertPos = parentNode.nodes.add(node);
            //TODO:设置文件夹
            //if (parentNode.nodeType() == DesignNodeType.FolderNode)
            //    entityModel.setFolderId(((FolderNode)parentNode).Folder.Id);
            //添加至根节点索引
            rootNode.addModelIndex(node);

            //设为签出状态
            node.setCheckoutInfo(new CheckoutInfo(node.nodeType(), node.checkoutInfoTargetID()
                    , entityModel.version(), hub.session.name(), hub.session.leafOrgUnitId()));
            //保存至Staged
            return node.saveAsync(null).thenApply(r -> {
                //新建虚拟文件
                hub.typeSystem.createModelDocument(node);
                var res = new NewNodeResult();
                res.ParentNodeType = parentNode.nodeType().value;
                res.ParentNodeID = parentNode.id();
                res.NewNode = node;
                res.RootNodeID = rootNodeHasCheckout ? null : rootNode.id();
                res.InsertIndex = insertPos;
                return res;
            });
        }).thenApply(JsonResult::new);
    }

}
