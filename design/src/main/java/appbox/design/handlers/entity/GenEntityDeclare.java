package appbox.design.handlers.entity;

import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.design.handlers.service.GenServiceDeclare;
import appbox.design.tree.ModelNode;
import appbox.model.EntityModel;
import appbox.model.ModelType;
import appbox.model.entity.DataFieldModel;
import appbox.model.entity.EntityMemberModel;
import appbox.model.entity.EntityRefModel;
import appbox.model.entity.EntitySetModel;
import appbox.runtime.InvokeArgs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GenEntityDeclare implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        String          modelId = args.getString();
        List<ModelNode> modelNodes;
        if (modelId == null || modelId.equals("")) { //空表示所有模型用于初次加载
            modelNodes = hub.designTree.findNodesByType(ModelType.Entity);
        } else { //指定标识用于更新
            long id   = Long.parseUnsignedLong(modelId);
            var  node = hub.designTree.findModelNode(ModelType.Entity, id);
            modelNodes = new ArrayList<>();
            modelNodes.add(node);
        }

        List<GenServiceDeclare.TypeScriptDeclare> list = new ArrayList<>();
        for (ModelNode node : modelNodes) {
            list.add(new GenServiceDeclare.TypeScriptDeclare(node.appNode.model.name(), buildDeclare(node, hub)));
        }

        return CompletableFuture.completedFuture(list);
    }

    private static String buildDeclare(ModelNode node, DesignHub hub) {
        var app   = node.appNode.model.name();
        var model = (EntityModel) node.model();
        var sb    = new StringBuilder();
        sb.append(String.format("declare namespace %s.Entities{{", app));
        sb.append(String.format("declare class %s extends EntityBase{{", model.name()));
        //注意：前端不关注成员是否readonly，以方便UI绑定如主键值
        for (EntityMemberModel m : model.getMembers()) {
            String type = "any";
            switch (m.type()) {
                case DataField:
                    type = GetDataFieldType((DataFieldModel) m);
                    break;
                case EntityRef: {
                    var rm = (EntityRefModel) m;
                    for (int i = 0; i < rm.getRefModelIds().size(); i++) {
                        var target   = hub.designTree.findModelNode(ModelType.Entity, rm.getRefModelIds().get(i));
                        var typeName = String.format("%s.Entities.%s", target.appNode.model.name(), target.model().name());
                        if (i == 0)
                            type = typeName;
                        else
                            type += String.format(" | %s", typeName);
                    }
                }
                break;
                case EntitySet: {
                    var sm     = (EntitySetModel) m;
                    var target = hub.designTree.findModelNode(ModelType.Entity, sm.refModelId());
                    type = String.format("%s.Entities.%s[]", target.appNode.model.name(), target.model().name());
                }
                break;
            }
            //TODO:处理注释
            sb.append(String.format("%s:%s;", m.name(), type));
        }
        sb.append("}}");
        return sb.toString();
    }

    private static String GetDataFieldType(DataFieldModel df) {
        switch (df.dataType()) {
            case EntityId:
            case Guid:
            case Binary:
            case String:
                return "string";
            case Bool:
                return "boolean";
            case Byte:
            case Decimal:
            case Double:
            case Enum:
            case Float:
            case Short:
            case Int:
            case Long:
                return "number";
            case DateTime:
                return "Date";
            default:
                return "any";
        }
    }
}
