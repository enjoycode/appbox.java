package appbox.channel.messages;

import appbox.serialization.IOutputStream;
import appbox.store.KVUtil;

public final class KVGetApplicationRequest extends KVGetRequest {
    private final int appId;

    public KVGetApplicationRequest(int appId) {
        this.appId = appId;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeInt(0); //ReqId占位
        bs.writeLong(KVUtil.META_RAFTGROUP_ID); //raftGroupId
        bs.writeByte((byte) -1);    //dataCF
        bs.writeLong(0);      //timestamp
        KVUtil.writeAppKey(bs, appId, false); //key
    }

}
