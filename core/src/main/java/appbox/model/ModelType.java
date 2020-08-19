package appbox.model;

public enum ModelType {
    Applicaton((byte) 0),
    Enum((byte) 1),
    Entity((byte) 2),
    Event((byte) 3),
    Service((byte) 4),
    View((byte) 5),
    Workflow((byte) 6),
    Report((byte) 7),
    Folder((byte) 8),
    Permission((byte) 9),
    DataStore((byte) 10);

    public final byte value;

    ModelType(byte v) {
        value = v;
    }
}
