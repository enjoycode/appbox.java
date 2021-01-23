package appbox.channel.messages;

import appbox.serialization.IOutputStream;
import appbox.store.KVUtil;

public final class KVGetModelCodeRequest extends KVGetRequest {
    private final long modelId;

    public KVGetModelCodeRequest(long modelId) {
        this.modelId = modelId;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeInt(0); //ReqId占位
        bs.writeLong(KVUtil.META_RAFTGROUP_ID); //raftGroupId
        bs.writeByte((byte) -1);    //dataCF
        bs.writeLong(0);      //timestamp
        KVUtil.writeModelCodeKey(bs, modelId, false); //key
    }
}
