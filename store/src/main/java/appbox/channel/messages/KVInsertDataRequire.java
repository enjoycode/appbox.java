package appbox.channel.messages;

import appbox.serialization.BinSerializer;

/**
 * 仅用于测试
 */
public final class KVInsertDataRequire extends KVInsertRequire {
    public byte[] key;
    public byte[] refs;
    public byte[] data;

    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        super.writeTo(bs);

        bs.writeByteArray(key);
        bs.writeByteArray(refs);
        if (data != null && data.length > 0) {
            bs.write(data, 0, data.length);
        }
    }
}
