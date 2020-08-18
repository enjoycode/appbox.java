package appbox.core.data;

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

}