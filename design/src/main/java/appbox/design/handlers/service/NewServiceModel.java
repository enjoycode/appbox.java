package appbox.design.handlers.service;

import appbox.design.DesignHub;
import appbox.design.common.CheckoutInfo;
import appbox.design.common.NewNodeResult;
import appbox.design.handlers.IDesignHandler;
import appbox.design.tree.DesignNodeType;
import appbox.design.tree.FolderNode;
import appbox.design.tree.ModelNode;
import appbox.design.utils.CodeHelper;
import appbox.model.ModelLayer;
import appbox.model.ModelType;
import appbox.model.ServiceModel;
import appbox.runtime.InvokeArgs;
import appbox.store.ModelStore;

import java.util.concurrent.CompletableFuture;

public final class NewServiceModel implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        // 获取接收到的参数
        int selectedNodeType = args.getInt();
        String selectedNodeId = args.getString();
        String name = args.getString();

        // 验证类名称的合法性
        if (name==null||name.equals("") || !CodeHelper.isValidIdentifier(name))
            throw new RuntimeException("Service name invalid");
        // 获取选择的节点
        var selectedNode = hub.designTree.findNode(DesignNodeType.fromValue((byte)selectedNodeType), selectedNodeId);
        if (selectedNode == null)
            throw new RuntimeException("Can't find selected node");

        var parentNode = hub.designTree.findNewModelParentNode(selectedNode, ModelType.Service);
        if (parentNode == null)
            throw new RuntimeException("Can't find parent node");
        //判断名称是否已存在
        if (hub.designTree.findModelNodeByName(parentNode.appId, ModelType.Service, name) != null)
            throw new RuntimeException("Service name has exists");

        //判断当前模型根节点有没有签出
        var rootNode = hub.designTree.findModelRootNode(parentNode.appId, ModelType.Service);
        boolean rootNodeHasCheckout = rootNode.isCheckoutByMe();
        return rootNode.checkout().thenApply(r->{
            if(!r){
                new RuntimeException(String.format("Can't checkout: %s",rootNode.fullName()));
            }
            //生成模型标识号并新建模型及节点
            return ModelStore.genModelIdAsync(parentNode.appId,ModelType.Service, ModelLayer.DEV).thenCompose(modelId->{
                var model = new ServiceModel(modelId, name);
                var node = new ModelNode(model, hub);
                var insertIndex = parentNode.designNode.nodes.add(node);
                //设置文件夹
                if (parentNode.designNode.nodeType() == DesignNodeType.FolderNode)
                    model.setFolderId(((FolderNode)parentNode.designNode).getFolder().getId());
                // 添加至根节点索引内
                rootNode.addModelIndex(node);

                //设为签出状态
                node.setCheckoutInfo( new CheckoutInfo(node.nodeType(), node.checkoutInfoTargetID(), model.version(),
                        hub.session.name(), hub.session.leafOrgUnitId()));

                //保存至Staged
                var appName = node.appNode.model.name();
                var initServiceCode = String.format("using System;\nusing System.Threading.Tasks;\n\nnamespace %s.ServiceLogic\n{{\n\tpublic class %s\n\t{{\n\t}}\n}}",appName,model.name());//TODO java version code
                return node.saveAsync(new Object[] { initServiceCode }).thenApply(re->{
                     hub.typeSystem.createModelDocument(node);
                     return CompletableFuture.completedFuture(new NewNodeResult((int)parentNode.designNode.nodeType().value,parentNode.designNode.id(),node,rootNodeHasCheckout ? null : rootNode.id(),insertIndex));
                });
            });
        });

    }
}
