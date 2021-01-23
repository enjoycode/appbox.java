package appbox.channel.messages;

import appbox.serialization.IOutputStream;
import appbox.store.KVTxnId;
import appbox.store.KVUtil;

public final class KVDeleteModelCodeRequest extends KVDeleteRequest {

    private final long _modelId;

    public KVDeleteModelCodeRequest(KVTxnId txnId, long modelId) {
        super(txnId);

        _modelId = modelId;

        raftGroupId   = KVUtil.META_RAFTGROUP_ID;
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
        KVUtil.writeModelCodeKey(bs, _modelId, false);
    }
}
