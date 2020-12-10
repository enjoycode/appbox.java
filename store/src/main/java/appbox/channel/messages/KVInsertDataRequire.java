package appbox.channel.messages;

import appbox.serialization.IOutputStream;
import appbox.store.KVTxnId;
import appbox.store.KeyUtil;

/**
 * 仅用于测试
 */
public final class KVInsertDataRequire extends KVInsertRequire {
    public byte[] key;
    public byte[] refs;
    public byte[] data;

    public KVInsertDataRequire(KVTxnId txnId) {
        super(txnId);

        raftGroupId = KeyUtil.META_RAFTGROUP_ID;
        dataCF      = -1;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        super.writeTo(bs);

        bs.writeByteArray(key);
        bs.writeByteArray(refs);
        if (data != null && data.length > 0) {
            bs.write(data, 0, data.length);
        }
    }
}
