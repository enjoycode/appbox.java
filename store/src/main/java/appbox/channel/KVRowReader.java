package appbox.channel;

import appbox.serialization.BinSerializer;

public final class KVRowReader {

    private final byte[] rowData;

    public KVRowReader(byte[] value) {
        rowData = value;
    }

    /** 将存储返回的成员值写入 */
    public void writeMember(short id, BinSerializer bs, byte flags) throws Exception {
        throw new RuntimeException("未实现");
    }

}
