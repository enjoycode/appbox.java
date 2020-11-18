package appbox.data;

/**
 * 数据的持久化状态
 */
public enum PersistentState {
    Detached((byte) 0),
    Unchnaged((byte) 1),
    Modified((byte) 2),
    Deleted((byte) 3);

    public final byte value;

    PersistentState(byte v) {
        value = v;
    }

    public static PersistentState fromValue(byte v) {
        switch (v) {
            case 0:
                return PersistentState.Detached;
            case 1:
                return PersistentState.Unchnaged;
            case 2:
                return PersistentState.Modified;
            case 3:
                return PersistentState.Deleted;
            default:
                throw new RuntimeException("Unknown PersistentState value: " + v);
        }
    }
}
