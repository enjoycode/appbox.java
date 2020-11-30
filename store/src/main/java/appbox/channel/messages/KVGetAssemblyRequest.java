package appbox.channel.messages;

import appbox.serialization.BinSerializer;
import appbox.store.KeyUtil;

public final class KVGetAssemblyRequest extends KVGetRequest {
    private final boolean isService;
    private final String  asmName;

    public KVGetAssemblyRequest(boolean isService, String asmName) {
        this.isService = isService;
        this.asmName   = asmName;
    }

    @Override
    public void writeTo(BinSerializer bs) {
        bs.writeInt(0); //ReqId占位
        bs.writeLong(KeyUtil.META_RAFTGROUP_ID); //raftGroupId
        bs.writeByte((byte) -1);    //dataCF
        bs.writeLong(0);      //timestamp
        KeyUtil.writeAssemblyKey(bs, isService, asmName); //key
    }
}
