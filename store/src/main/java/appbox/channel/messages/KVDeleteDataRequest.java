package appbox.channel.messages;

import appbox.serialization.BinSerializer;
import appbox.store.KVTxnId;

/** 仅用于测试 */
public final class KVDeleteDataRequest extends KVDeleteRequest {

    public byte[] key;
    public byte[] refs;

    public KVDeleteDataRequest(KVTxnId txnId) {
        super(txnId);

        raftGroupId = 0;
        dataCF      = -1;
    }

    @Override
    public void writeTo(BinSerializer bs) {
        super.writeTo(bs);

        //refs
        bs.writeByteArray(refs);
        //key
        bs.write(key, 0, key.length);
    }
}
