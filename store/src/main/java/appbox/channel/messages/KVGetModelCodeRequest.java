package appbox.channel.messages;

import appbox.serialization.BinSerializer;
import appbox.serialization.IOutputStream;
import appbox.store.KeyUtil;

public final class KVGetModelCodeRequest extends KVGetRequest {
    private final long modelId;

    public KVGetModelCodeRequest(long modelId) {
        this.modelId = modelId;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeInt(0); //ReqId占位
        bs.writeLong(KeyUtil.META_RAFTGROUP_ID); //raftGroupId
        bs.writeByte((byte) -1);    //dataCF
        bs.writeLong(0);      //timestamp
        KeyUtil.writeModelCodeKey(bs, modelId, false); //key
    }
}
