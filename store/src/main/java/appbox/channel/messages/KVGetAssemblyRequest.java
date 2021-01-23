package appbox.channel.messages;

import appbox.serialization.IOutputStream;
import appbox.store.KVUtil;

public final class KVGetAssemblyRequest extends KVGetRequest {
    private final boolean isService;
    private final String  asmName;

    public KVGetAssemblyRequest(boolean isService, String asmName) {
        this.isService = isService;
        this.asmName   = asmName;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeInt(0); //ReqId占位
        bs.writeLong(KVUtil.META_RAFTGROUP_ID); //raftGroupId
        bs.writeByte((byte) -1);    //dataCF
        bs.writeLong(0);      //timestamp
        KVUtil.writeAssemblyKey(bs, isService, asmName, false); //key
    }
}
