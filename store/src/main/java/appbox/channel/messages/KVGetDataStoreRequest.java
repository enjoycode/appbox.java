package appbox.channel.messages;

import appbox.serialization.IOutputStream;
import appbox.store.KVUtil;

public class KVGetDataStoreRequest extends KVGetRequest {
    private final long modelId;

    public KVGetDataStoreRequest(long modelId) {
        this.modelId = modelId;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeInt(0); //ReqId占位
        bs.writeLong(KVUtil.META_RAFTGROUP_ID); //raftGroupId
        bs.writeByte((byte) -1);    //dataCF
        bs.writeLong(0);      //timestamp
        KVUtil.writeDataStoreKey(bs, modelId, false); //key
    }
}
