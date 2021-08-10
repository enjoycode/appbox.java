package appbox.design.lang.java.utils;

import appbox.model.ModelType;

public final class ModelTypeUtil {

    public static String toLowercaseTypeName(ModelType type) {
        switch (type) {
            case Enum:
                return "enums";
            case Entity:
                return "entities";
            case Event:
                return "events";
            case Service:
                return "services";
            case View:
                return "views";
            case Workflow:
                return "workflows";
            case Report:
                return "reports";
            case Permission:
                return "permissions";
            default:
                return "unknown";
        }
    }
}
