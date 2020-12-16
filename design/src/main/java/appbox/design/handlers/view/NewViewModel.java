package appbox.design.handlers.view;

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
import appbox.model.ModelLayer;
import appbox.model.ModelType;
import appbox.model.ViewModel;
import appbox.runtime.InvokeArgs;
import appbox.store.ModelStore;

import java.util.concurrent.CompletableFuture;

public class NewViewModel implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        //读取参数
        int selectedNodeType = args.getInt();
        String selectedNodeId = args.getString();
        String newname = args.getString();

        //先判断名称有效性
        if (newname==null||newname.equals(""))
            throw new RuntimeException("名称不能为空");
        if (!CodeHelper.isValidIdentifier(newname))
            throw new RuntimeException("名称包含无效字符");

        //获取选择的节点
        var selectedNode = hub.designTree.findNode(DesignNodeType.fromValue((byte)selectedNodeType), selectedNodeId);
        if (selectedNode == null)
            throw new RuntimeException("无法找到当前节点");

        //根据选择的节点获取合适的插入位置
        var parentNode = hub.designTree.findNewModelParentNode(selectedNode, ModelType.View);
        if (parentNode == null)
            throw new RuntimeException("无法找到当前节点的上级节点");
        var appNode = DesignTree.findAppNodeFromNode(parentNode);
        //判断名称是否已存在
        if (hub.designTree.findModelNodeByName(appNode.model.id(), ModelType.View, newname) != null)
            throw new RuntimeException("View name has exists");

        //判断当前模型根节点有没有签出
        var rootNode = hub.designTree.findModelRootNode(appNode.model.id(), ModelType.View);
        boolean rootNodeHasCheckout = rootNode.isCheckoutByMe();

        return rootNode.checkout().thenCompose(r->{
            if(!r){
                throw new RuntimeException(String.format("Can't checkout: %s",rootNode.fullName()));
            }
            //生成模型标识号并新建模型及节点 //TODO:fix Layer
            return ModelStore.genModelIdAsync(appNode.model.id(), ModelType.View, ModelLayer.DEV).thenCompose(modelId->{
                var model = new ViewModel(modelId, newname);
                var node = new ModelNode(model, hub);
                //添加至设计树
                var insertIndex = parentNode.nodes.add(node);
                //设置文件夹
                if (parentNode.nodeType() == DesignNodeType.FolderNode)
                    model.setFolderId(((FolderNode)parentNode).getFolder().getId());
                //添加至根节点索引内
                rootNode.addModelIndex(node);

                //设为签出状态
                node.setCheckoutInfo(new CheckoutInfo(node.nodeType(), node.checkoutInfoTargetID(), model.version(),
                        hub.session.name(), hub.session.leafOrgUnitId()));

                //保存至本地
                var templateCode = "<div>Hello Future!</div>";
                var scriptCode = String.format("@Component\nexport default class %s extends Vue {{\n\n}}\n",model.name());
                return node.saveAsync(new Object[] { templateCode, scriptCode, "", "" })
                        .thenApply(re-> new NewNodeResult(parentNode.nodeType().value,parentNode.id(),node,rootNodeHasCheckout ? null : rootNode.id(),insertIndex));
            });
        }).thenApply(JsonResult::new);
    }
}
