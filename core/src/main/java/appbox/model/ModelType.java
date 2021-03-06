package appbox.model;

public enum ModelType {
    Enum((byte) 1),
    Entity((byte) 2),
    Event((byte) 3),
    Service((byte) 4),
    View((byte) 5),
    Workflow((byte) 6),
    Report((byte) 7),
    Folder((byte) 8),
    Permission((byte) 9);

    public final byte value;

    ModelType(byte v) {
        value = v;
    }

    public static ModelType fromValue(byte v) {
        for (ModelType item : ModelType.values()) {
            if (item.value == v) {
                return item;
            }
        }
        throw new RuntimeException("Unknown value: " + v);
    }
}
