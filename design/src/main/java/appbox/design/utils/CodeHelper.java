package appbox.design.utils;

import appbox.model.ModelType;

public final class CodeHelper {

    public static boolean isValidIdentifier(String name) {
        //TODO:
        return true;
    }

    /**
     * 获取模型类型的复数名称
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
            case Applicaton:
                return "Applications";
            default:
                return "Unknown";
        }
    }
}
