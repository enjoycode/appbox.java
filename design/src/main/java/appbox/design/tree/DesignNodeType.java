package appbox.design.tree;

public enum DesignNodeType {
    ApplicationRoot((byte) 0),
    DataStoreRootNode((byte) 1),
    DataStoreNode((byte) 2),
    ApplicationNode((byte) 3),
    ModelRootNode((byte) 4),
    FolderNode((byte) 6),

    BlobStoreNode((byte) 10),

    EntityModelNode((byte) 20),
    ServiceModelNode((byte) 21),
    ViewModelNode((byte) 22),
    EnumModelNode((byte) 23),
    EventModelNode((byte) 24),
    PermissionModelNode((byte) 25),
    WorkflowModelNode((byte) 26),
    ReportModelNode((byte) 27);

    public final byte value;

    DesignNodeType(byte value) {
        this.value = value;
    }

    public static DesignNodeType fromValue(byte value) {
        for(var item : DesignNodeType.values()) {
            if (item.value == value)
                return item;
        }
        throw new RuntimeException("Unknown DesignNodeType: " + value);
    }
}
