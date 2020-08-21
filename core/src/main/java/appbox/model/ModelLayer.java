package appbox.model;

public enum ModelLayer {
    SYS((byte) 0),
    DEV((byte) 1),
    USR((byte) 2);

    public final byte value;

    ModelLayer(byte v) {
        value = v;
    }

    public static ModelLayer fromValue(byte value) {
        switch (value) {
            case 0:
                return ModelLayer.SYS;
            case 1:
                return ModelLayer.DEV;
            default:
                return ModelLayer.USR;
        }
    }
}
