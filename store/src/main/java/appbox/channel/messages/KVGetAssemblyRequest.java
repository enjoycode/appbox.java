package appbox.channel.messages;

import appbox.serialization.IOutputStream;
import appbox.store.KVUtil;
import appbox.store.MetaAssemblyType;

public final class KVGetAssemblyRequest extends KVGetRequest {
    private final MetaAssemblyType asmType;
    private final String           asmName;

    public KVGetAssemblyRequest(MetaAssemblyType asmType, String asmName) {
        this.asmType = asmType;
        this.asmName = asmName;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeInt(0); //ReqId占位
        bs.writeLong(KVUtil.META_RAFTGROUP_ID); //raftGroupId
        bs.writeByte((byte) -1);    //dataCF
        bs.writeLong(0);      //timestamp
        KVUtil.writeAssemblyKey(bs, asmType, asmName, false); //key
    }
}
