package appbox.design.handlers.enumm;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.design.common.CheckoutInfo;
import appbox.design.common.NewNodeResult;
import appbox.design.handlers.IDesignHandler;
import appbox.design.tree.DesignNodeType;
import appbox.design.tree.DesignTree;
import appbox.design.tree.FolderNode;
import appbox.design.tree.ModelNode;
import appbox.design.utils.CodeHelper;
import appbox.model.EnumModel;
import appbox.model.ModelLayer;
import appbox.model.ModelType;
import appbox.runtime.InvokeArgs;
import appbox.store.ModelStore;

import java.util.concurrent.CompletableFuture;

public class NewEnumModel implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        // 获取接收到的参数
        int selectedNodeType = args.getInt();
        String selectedNodeId = args.getString();
        var name = args.getString();

        // 验证类名称的合法性
        if (name==null||name.equals("") || !CodeHelper.isValidIdentifier(name))
            throw new RuntimeException("Enum name invalid");
        //获取选择的节点
        var selectedNode = hub.designTree.findNode(DesignNodeType.fromValue((byte)selectedNodeType), selectedNodeId);
        if (selectedNode == null)
            throw new RuntimeException("Can't find selected node");

        //根据选择的节点获取合适的插入位置
        var parentNode = hub.designTree.findNewModelParentNode(selectedNode, ModelType.Enum);
        if (parentNode == null)
            throw new RuntimeException("Can't find parent node");
        var appNode = DesignTree.findAppNodeFromNode(parentNode);
        //判断名称是否已存在
        if (hub.designTree.findModelNodeByName(appNode.model.id(), ModelType.Enum, name) != null)
            throw new RuntimeException("Enum name has exists");

        //判断当前模型根节点有没有签出
        var rootNode = hub.designTree.findModelRootNode(appNode.model.id(), ModelType.Enum);
        boolean rootNodeHasCheckout = rootNode.isCheckoutByMe();
        return rootNode.checkout().thenCompose(r-> {
            if (!r) {
                throw new RuntimeException(String.format("Can't checkout: %s", rootNode.fullName()));
            }
            //生成模型标识号并新建模型及节点 //TODO:fix Layer
            return ModelStore.genModelIdAsync(appNode.model.id(), ModelType.Enum, ModelLayer.DEV).thenCompose(modelId->{
                var model = new EnumModel(modelId, name);
                var node = new ModelNode(model, hub);
                //添加至设计树
                var insertIndex = parentNode.nodes.add(node);
                //设置文件夹
                if (parentNode.nodeType() == DesignNodeType.FolderNode)
                    model.setFolderId(((FolderNode)parentNode).getFolder().getId());
                // 添加至根节点索引内
                rootNode.addModelIndex(node);

                //设为签出状态
                node.setCheckoutInfo(new CheckoutInfo(node.nodeType(), node.checkoutInfoTargetID(), model.version(),
                        hub.session.name(), hub.session.leafOrgUnitId()));
                return node.saveAsync(null)
                        .thenApply(re-> {
                            //新建虚拟文件
                            hub.typeSystem.createModelDocument(node);
                            return new NewNodeResult(parentNode.nodeType().value, parentNode.id(), node, rootNodeHasCheckout ? null : rootNode.id(), insertIndex);
                        });
            });

        }).thenApply(JsonResult::new);

    }
}
