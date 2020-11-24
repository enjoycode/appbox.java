package appbox.design.tree;

import appbox.design.DesignHub;
import appbox.model.ModelBase;
import appbox.model.ModelType;
import appbox.model.ServiceModel;
import com.alibaba.fastjson.JSONWriter;

/**
 * 模型节点
 */
public final class ModelNode extends DesignNode {
    private      ModelBase       _model;
    public final ApplicationNode appNode;

    public ModelNode(ModelBase targetModel, DesignHub hub) { //注意：新建时尚未加入树，无法获取TreeView实例
        appNode = hub.designTree.findApplicationNode(targetModel.appId());
        _model  = targetModel;
        text    = targetModel.name();
    }

    public ModelBase model() {
        return _model;
    }

    void setModel(ModelBase newModel) {
        _model = newModel;
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

    @Override
    protected void writeJsonMembers(JSONWriter writer) {
        super.writeJsonMembers(writer);

        writer.writeKey("App"); //TODO:考虑不用，由前端处理
        writer.writeValue(appNode.model.name());

        writer.writeKey("ModelType");
        writer.writeValue(_model.modelType().value);

        //TODO: EntityModel输出对应的存储标识，方便前端IDE筛选相同存储的实体
        //ServiceModel输出Language
        if (_model.modelType() == ModelType.Service) {
            writer.writeKey("Language");
            writer.writeValue(((ServiceModel) _model).language().value);
        }
    }
}