package appbox.design;

public enum DesignNodeType
{
    ApplicationRoot((byte)0),
    DataStoreRootNode((byte)1),
    DataStoreNode((byte)2),
    ApplicationNode((byte)3),
    ModelRootNode((byte)4),
    FolderNode((byte)6),

    BlobStoreNode((byte)10),

    EntityModelNode((byte)20),
    ServiceModelNode((byte)21),
    ViewModelNode((byte)22),
    EnumModelNode((byte)23),
    EventModelNode((byte)24),
    PermissionModelNode((byte)25),
    WorkflowModelNode((byte)26),
    ReportModelNode((byte)27);

    public static final int SIZE = java.lang.Byte.SIZE;

    private byte byteValue;
    private static java.util.HashMap<Byte, DesignNodeType> mappings;
    private static java.util.HashMap<Byte, DesignNodeType> getMappings()
    {
        if (mappings == null)
        {
            synchronized (DesignNodeType.class)
            {
                if (mappings == null)
                {
                    mappings = new java.util.HashMap<Byte, DesignNodeType>();
                }
            }
        }
        return mappings;
    }

    private DesignNodeType(byte value)
    {
        byteValue = value;
        getMappings().put(value, this);
    }

    public byte getValue()
    {
        return byteValue;
    }

    public static DesignNodeType forValue(byte value)
    {
        return getMappings().get(value);
    }
}
