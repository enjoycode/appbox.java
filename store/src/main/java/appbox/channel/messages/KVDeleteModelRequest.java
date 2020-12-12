package appbox.channel.messages;

import appbox.serialization.IOutputStream;
import appbox.store.KVTxnId;
import appbox.store.KeyUtil;

public final class KVDeleteModelRequest extends KVDeleteRequest {

    private final long _modelId;

    public KVDeleteModelRequest(KVTxnId txnId, long modelId) {
        super(txnId);

        _modelId = modelId;

        raftGroupId   = KeyUtil.META_RAFTGROUP_ID;
        schemaVersion = 0;
        returnExists  = false;
        dataCF        = -1;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        super.writeTo(bs);

        //refs always 0
        bs.writeVariant(0);

        //key(不带长度)
        KeyUtil.writeModelKey(bs, _modelId, false);
    }
}
