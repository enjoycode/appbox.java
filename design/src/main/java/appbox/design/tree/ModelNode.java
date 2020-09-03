package appbox.design.tree;

import appbox.design.DesignHub;
import appbox.design.tree.DesignNode;
import appbox.design.tree.DesignNodeType;
import appbox.model.ModelBase;

public final class ModelNode extends DesignNode {
    private         ModelBase       _model;
    protected final ApplicationNode appNode;

    public ModelNode(ModelBase targetModel, DesignHub hub) { //注意：新建时尚未加入树，无法获取TreeView实例
        appNode = hub.designTree.findApplicationNode(targetModel.appId());
        _model  = targetModel;
        text    = targetModel.name();
    }

    public ModelBase model() {
        return _model;
    }

    @Override
    public String id() {
        return Long.toUnsignedString(_model.id());
    }

    @Override
    public DesignNodeType nodeType() {
        switch (_model.modelType()) {
            case Entity:
                return DesignNodeType.EntityModelNode;
            case Service:
                return DesignNodeType.ServiceModelNode;
            case View:
                return DesignNodeType.ViewModelNode;
            case Workflow:
                return DesignNodeType.WorkflowModelNode;
            case Report:
                return DesignNodeType.ReportModelNode;
            case Enum:
                return DesignNodeType.EnumModelNode;
            case Permission:
                return DesignNodeType.PermissionModelNode;
            case Event:
                return DesignNodeType.EventModelNode;
            default:
                throw new RuntimeException("Unknow design node type.");
        }
    }
}