package appbox.design.tree;

import appbox.data.PersistentState;
import appbox.design.DesignHub;
import appbox.design.services.StagedService;
import appbox.model.EntityModel;
import appbox.model.ModelBase;
import appbox.model.ModelType;
import appbox.model.ServiceModel;
import appbox.serialization.IJsonWriter;

import java.util.concurrent.CompletableFuture;

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

    //region ====Properties====
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
    public int version() {
        return _model.version();
    }

    @Override
    public String checkoutInfoTargetID() {
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
    //endregion

    @Override
    protected void writeJsonMembers(IJsonWriter writer) {
        super.writeJsonMembers(writer);

        writer.writeKeyValue("App",appNode.model.name()); //TODO:考虑不用，由前端处理
        writer.writeKeyValue("ModelType",_model.modelType().value);

        if (_model.modelType() == ModelType.Entity) {
            //EntityModel输出对应的存储标识，方便前端IDE筛选相同存储的实体
            final var entityModel = (EntityModel) model();
            if (entityModel.sysStoreOptions() != null) {
                writer.writeKeyValue("StoreId", 0);
            } else if (entityModel.sqlStoreOptions() != null) {
                writer.writeKeyValue("StoreId", Long.toUnsignedString(entityModel.sqlStoreOptions().storeModelId()));
            } //TODO:Cql
        } else if (_model.modelType() == ModelType.Service) {
            //ServiceModel输出Language
            writer.writeKey("Language");
            writer.writeValue(((ServiceModel) _model).language().value);
        }
    }

    /** 保存模型节点 */
    public CompletableFuture<Void> saveAsync(Object[] modelInfos) {
        if (!isCheckoutByMe()) {
            return CompletableFuture.failedFuture(new RuntimeException("ModelNode has not checkout"));
        }

        //TODO:考虑事务保存模型及相关代码

        return StagedService.saveModelAsync(_model).thenCompose(r -> {
            //更新相关模型的内容
            if (_model.persistentState() != PersistentState.Deleted) {
                switch (_model.modelType()) {
                    case Service: {
                        //TODO:更新服务模型代理类
                        //保存服务模型代码
                        String sourceCode;
                        if (modelInfos != null && modelInfos.length == 1)
                        {
                            sourceCode = (String)modelInfos[0];
                        }
                        else {
                            var doc = designTree().designHub.typeSystem.languageServer.findOpenedDocument(_model.id());
                            sourceCode = doc.getContents();
                        }
                        return StagedService.saveServiceCodeAsync(_model.id(), sourceCode);
                    }
                    case View:
                    {
                        //View模型保存
                        if (modelInfos != null)
                        {
                            return StagedService.saveViewCodeAsync(_model.id(), (String)modelInfos[0], (String)modelInfos[1], (String)modelInfos[2])
                                    .thenCompose(re->StagedService.saveViewRuntimeCodeAsync(_model.id(), (String)modelInfos[3]));
                        }
                    }
                    default:
                        return CompletableFuture.completedFuture(null);
                }
            } else {
                return CompletableFuture.completedFuture(null);
            }
        });
    }

}
