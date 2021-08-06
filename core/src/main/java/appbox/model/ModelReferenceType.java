package appbox.model;

/** 模型引用的类型 */
public enum ModelReferenceType {
    ApplicationId((byte) 0),
    EntityModelId((byte) 1),
    EntityMemberName((byte) 2),
    EntityIndexName((byte) 3),
    ServiceModelId((byte) 4),
    ServiceMethodName((byte) 5),
    EnumModelId((byte) 6),
    EnumModelItemName((byte) 7),
    ViewModelId((byte) 8),
    ReportModelId((byte) 9),
    WorkflowModelId((byte) 10),
    PermissionModelId((byte) 11),
    EventModelId((byte) 12);

    public final byte value;

    ModelReferenceType(byte v) {
        value = v;
    }

}
