package appbox.channel.messages;

import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.store.KeyUtil;

/**
 * 从存储加载单个模型，应用模型及普通模型通用
 */
public final class KVGetModelRequest extends KVGetRequest {
    private final long modelId;
    private final KVReadDataType type;

    public KVGetModelRequest(long modelId, KVReadDataType type) {
        this.modelId = modelId;
        this.type    = type;
    }

    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        bs.writeInt(0); //ReqId占位
        bs.writeLong(KeyUtil.META_RAFTGROUP_ID); //raftGroupId
        if (type == KVReadDataType.ApplicationModel) {
            KeyUtil.writeAppKey(bs, (int) modelId, true); //key
        } else if (type == KVReadDataType.Model) {
            KeyUtil.writeModelKey(bs, modelId); //key
        } else {
            KeyUtil.writeModelCodeKey(bs, modelId); //key
        }
        bs.writeByte((byte) -1);    //dataCF
        bs.writeByte(type.value);         //dataType
        bs.writeLong(0);      //timestamp
    }
}
