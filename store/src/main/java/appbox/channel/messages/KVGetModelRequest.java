package appbox.channel.messages;

import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.store.KeyUtil;

public final class KVGetModelRequest extends KVGetRequest {
    private final long _modelId;

    public KVGetModelRequest(long modelId) {
        _modelId = modelId;
    }

    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        bs.writeInt(0); //ReqId占位
        bs.writeLong(KeyUtil.META_RAFTGROUP_ID); //raftGroupId
        KeyUtil.writeModelKey(bs, _modelId); //key
        bs.writeByte((byte) -1);    //dataCF
        bs.writeByte((byte) 1);     //dataType
        bs.writeLong(0);      //timestamp
    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {
        throw new RuntimeException("Not supported.");
    }
}
