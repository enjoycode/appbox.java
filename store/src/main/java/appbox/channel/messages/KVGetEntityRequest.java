package appbox.channel.messages;

import appbox.data.EntityId;
import appbox.serialization.IOutputStream;
import appbox.store.KeyUtil;

public final class KVGetEntityRequest extends KVGetRequest {
    private final EntityId entityId;

    public KVGetEntityRequest(EntityId id) {
        entityId = id;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeInt(0); //ReqId占位
        bs.writeLong(entityId.raftGroupId()); //raftGroupId
        bs.writeByte((byte) -1);    //dataCF
        bs.writeLong(0);      //timestamp
        KeyUtil.writeEntityKey(bs, entityId, false); //key
    }
}
