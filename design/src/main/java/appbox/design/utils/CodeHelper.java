package appbox.design.utils;

import appbox.model.ModelType;

public final class CodeHelper {

    public static boolean isValidIdentifier(String name) {
        //TODO:
        return true;
    }

    /**
     * 获取模型类型的复数名称(大驼峰)
     */
    public static String getPluralStringOfModelType(ModelType modelType) {
        switch (modelType) {
            case Enum:
                return "Enums";
            case Entity:
                return "Entities";
            case Event:
                return "Events";
            case Service:
                return "Services";
            case View:
                return "Views";
            case Workflow:
                return "Workflows";
            case Report:
                return "Reports";
            case Permission:
                return "Permissions";
            default:
                return "Unknown";
        }
    }

    public static ModelType getModelTypeFromLCC(String type) {
        switch (type) {
            case "entities":
                return ModelType.Entity;
            case "enums":
                return ModelType.Enum;
            case "services":
                return ModelType.Service;
            case "views":
                return ModelType.View;
            case "events":
                return ModelType.Event;
            case "permissions":
                return ModelType.Permission;
            case "reports":
                return ModelType.Report;
            case "workflows":
                return ModelType.Workflow;
            default:
                throw new RuntimeException("Unknown type: " + type);
        }
    }

}
