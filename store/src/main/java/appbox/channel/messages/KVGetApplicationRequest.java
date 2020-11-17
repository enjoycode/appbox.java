package appbox.channel.messages;

import appbox.serialization.BinSerializer;
import appbox.store.KeyUtil;

public final class KVGetApplicationRequest extends KVGetRequest {
    private final int appId;

    public KVGetApplicationRequest(int appId) {
        this.appId = appId;
    }

    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        bs.writeInt(0); //ReqId占位
        bs.writeLong(KeyUtil.META_RAFTGROUP_ID); //raftGroupId
        bs.writeByte((byte) -1);    //dataCF
        bs.writeLong(0);      //timestamp
        KeyUtil.writeAppKey(bs, appId, false); //key
    }

}
