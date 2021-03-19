package appbox.design.handlers.tree;

import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.design.services.StagedService;
import appbox.design.tree.*;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

/** 处理前端设计树拖放节点 */
public final class DragDropNode implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var sourceNodeType = DesignNodeType.fromValue((byte) args.getInt());
        var sourceNodeId   = args.getString();
        var targetNodeType = DesignNodeType.fromValue((byte) args.getInt());
        var targetNodeId   = args.getString();
        var position       = args.getString(); //inner or before or after

        var sourceNode = hub.designTree.findNode(sourceNodeType, sourceNodeId);
        var targetNode = hub.designTree.findNode(targetNodeType, targetNodeId);
        if (sourceNode == null || targetNode == null)
            throw new RuntimeException("Can't find drag or drop node");

        //TODO: 再次验证是否允许操作，前端已验证过
        //TODO:完整实现以下逻辑，暂只支持Inside
        if (position.equals("inner")) {
            switch (sourceNodeType) {
                case FolderNode:
                    throw new RuntimeException("未实现");
                case EntityModelNode:
                case ServiceModelNode:
                case ViewModelNode:
                case EnumModelNode:
                case EventModelNode:
                case PermissionModelNode:
                case WorkflowModelNode:
                case ReportModelNode:
                    return dropModelNodeInside((ModelNode) sourceNode, targetNode).thenApply(r -> null);
                default:
                    throw new RuntimeException("Not supported drag and drop");
            }
        } else {
            throw new RuntimeException("暂未实现");
        }
    }

    private static CompletableFuture<Void> dropModelNodeInside(ModelNode sourceNode, DesignNode targetNode) {
        //注意目标节点可能是模型根目录
        if (targetNode.nodeType() == DesignNodeType.ModelRootNode) {
            var rootNode = (ModelRootNode) targetNode;
            var appNode  = (ApplicationNode) rootNode.getParent();
            if (appNode.model.id() != sourceNode.model().appId())
                throw new UnsupportedOperationException("Can't drop to other application");

            sourceNode.getParent().nodes.remove(sourceNode);
            targetNode.nodes.add(sourceNode);
            sourceNode.model().setFolderId(null);
            return StagedService.saveModelAsync(sourceNode.model()); //直接保存
        } else if (targetNode.nodeType() == DesignNodeType.FolderNode) {
            var targetFolder = ((FolderNode)targetNode).folder;
            var rootFolder = targetFolder.getRoot();
            if (rootFolder.appId() != sourceNode.model().appId())
                throw new UnsupportedOperationException("Can't drop to other application");

            sourceNode.getParent().nodes.remove(sourceNode);
            targetNode.nodes.add(sourceNode);
            sourceNode.model().setFolderId(targetFolder.id());
            return StagedService.saveModelAsync(sourceNode.model()); //直接保存
        } else {
            throw new UnsupportedOperationException();
        }
    }

}
