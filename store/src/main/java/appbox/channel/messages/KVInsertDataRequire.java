package appbox.channel.messages;

import appbox.serialization.BinSerializer;
import appbox.store.KVTxnId;

/**
 * 仅用于测试
 */
public final class KVInsertDataRequire extends KVInsertRequire {
    public byte[] key;
    public byte[] refs;
    public byte[] data;

    public KVInsertDataRequire(KVTxnId txnId) {
        super(txnId);
    }

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
