package appbox.channel.messages;

import appbox.serialization.IOutputStream;
import appbox.store.KVTxnId;

/** 仅适用于更新索引的StoringFields, 即IndexKey没有变化 */
public final class KVUpdateIndexRequest extends KVUpdateRequest {



    public KVUpdateIndexRequest(KVTxnId txnId) {
        super(txnId);
    }

    @Override
    public void writeTo(IOutputStream bs) {
        super.writeTo(bs);


    }

}
