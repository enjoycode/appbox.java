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

    public static ModelType fromLowercaseType(String typeName) {
        switch (typeName) {
            case "enums":
                return ModelType.Enum;
            case "entities":
                return ModelType.Entity;
            case "events":
                return ModelType.Event;
            case "services":
                return ModelType.Service;
            case "views":
                return ModelType.View;
            case "workflows":
                return ModelType.Workflow;
            case "reports":
                return ModelType.Report;
            case "permissions":
                return ModelType.Permission;
            default:
                throw new RuntimeException("Unknown ModelType: " + typeName);
        }
    }
}
