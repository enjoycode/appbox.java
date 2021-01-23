package appbox.channel.messages;

import appbox.serialization.IOutputStream;
import appbox.store.KVTxnId;
import appbox.store.KVUtil;
import appbox.utils.IdUtil;

public final class KVInsertModelCodeRequire extends KVInsertRequire {
    private final long   modelId;
    private final byte[] codeData;

    public KVInsertModelCodeRequire(KVTxnId txnId, long modelId, byte[] codeData) {
        super(txnId);

        this.modelId  = modelId;
        this.codeData = codeData;

        raftGroupId    = KVUtil.META_RAFTGROUP_ID;
        schemaVersion  = 0;
        dataCF         = -1;
        overrideExists = true;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        super.writeTo(bs);

        //key
        KVUtil.writeModelCodeKey(bs, modelId, true);
        //refs
        bs.writeVariant(0);
        //data
        bs.writeByte(IdUtil.getModelTypeFromModelId(modelId).value); //注意写入模型类型信息
        bs.write(codeData, 0, codeData.length);
    }
}
