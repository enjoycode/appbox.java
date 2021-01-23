package appbox.store;

public final class PartitionInfo {

    public final byte[] key;
    public final byte   flags;

    public PartitionInfo(int size, byte flags) {
        this.key   = new byte[size];
        this.flags = flags;
    }

    /** 编码全局表的分区键 */
    public void encodeGlobalTablePartitionKey(byte appStoreId, int tableStoreId) {
        key[0] = appStoreId;
        key[1] = (byte) ((tableStoreId >>> 16) & 0xFF);
        key[2] = (byte) ((tableStoreId >>> 8) & 0xFF);
        key[3] = (byte) (tableStoreId & 0xFF);
        key[4] = KVUtil.PARTCF_GLOBAL_TABLE_FLAG;
    }

}
