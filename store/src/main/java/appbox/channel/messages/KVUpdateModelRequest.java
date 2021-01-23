package appbox.channel.messages;

import appbox.model.ModelBase;
import appbox.serialization.IOutputStream;
import appbox.store.KVTxnId;
import appbox.store.KVUtil;

public final class KVUpdateModelRequest extends KVUpdateRequest {

    private final ModelBase model;

    public KVUpdateModelRequest(KVTxnId txnId, ModelBase model) {
        super(txnId);

        this.model = model;

        raftGroupId   = KVUtil.META_RAFTGROUP_ID;
        schemaVersion = 0;
        dataCF        = -1;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        super.writeTo(bs);

        //key
        KVUtil.writeModelKey(bs, model.id(), true);
        //refs
        bs.writeVariant(0);
        //data
        bs.writeByte(model.modelType().value); //注意写入模型类型信息
        model.writeTo(bs);
    }
}
