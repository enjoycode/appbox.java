package appbox.design.services;

import appbox.data.EntityId;
import appbox.design.tree.DesignTree;
import appbox.model.EntityModel;
import appbox.model.entity.DataFieldModel;
import appbox.model.entity.EntityRefModel;
import appbox.model.entity.EntitySetModel;
import appbox.utils.StringUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用于生成各模型的虚拟代码
 */
public class CodeGenService {

    /**
     * 根据实体模型生成虚拟代码
     * @param model
     * @param appName
     * @param designTree
     * @return
     */
    public static String genEntityDummyCode(EntityModel model, String appName, DesignTree designTree) {
        var sb = new StringBuilder();
        sb.append("package ");
        sb.append(appName);
        sb.append(".entities\n");

        var className = StringUtil.firstUpperCase(model.name());
        sb.append("public class ");
        sb.append(className);
        sb.append(" {\n");

        //fields
        for (var memberModel : model.getMembers()) {
            switch (memberModel.type()) {
                case DataField:
                    genDataFieldMember((DataFieldModel) memberModel, sb);
                    break;
                case EntityRef:
                    genEntityRefMember((EntityRefModel) memberModel, sb);
                    break;
                case EntitySet:
                    genEntitySetMember((EntitySetModel) memberModel, sb);
                    break;
            }
        }

        sb.append("}");
        return sb.toString();
    }

    private static void genDataFieldMember(DataFieldModel field, StringBuilder sb) {
        sb.append("\tpublic ");
        sb.append(getDataFieldTypeString(field));
        sb.append(" ");
        sb.append(field.name());
        sb.append(";\n");
    }

    private static void genEntityRefMember(EntityRefModel entityRef, StringBuilder sb) {

    }

    private static void genEntitySetMember(EntitySetModel entitySet, StringBuilder sb) {

    }

    private static String getDataFieldTypeString(DataFieldModel field) {
        switch (field.dataType()) {
            case EntityId:
                return EntityId.class.getName();
            case String:
                return "String";
            case DateTime:
                return LocalDateTime.class.getName();
            case Short:
                return field.allowNull() ? "Short" : "short";
            case Int:
            case Enum:
                return field.allowNull() ? "Integer" : "int";
            case Long:
                return field.allowNull() ? "Long" : "long";
            case Decimal:
                return BigDecimal.class.getName();
            case Bool:
                return field.allowNull() ? "Boolean" : "boolean";
            case Guid:
                return UUID.class.getName();
            case Byte:
                return field.allowNull() ? "Byte" : "byte";
            case Binary:
                return "byte[]";
            case Float:
                return field.allowNull() ? "Float" : "float";
            case Double:
                return field.allowNull() ? "Double" : "double";
            default:
                return "Object";
        }
    }

}
