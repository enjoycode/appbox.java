package appbox.channel.messages;

import appbox.serialization.IOutputStream;
import appbox.store.KVUtil;

public final class KVScanAppsRequest extends KVScanRequest {
    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeInt(0); //ReqId占位
        bs.writeLong(KVUtil.META_RAFTGROUP_ID); //raftGroupId
        bs.writeNativeVariant(1); //BeginKeySize
        bs.writeByte(KVUtil.METACF_APP_PREFIX); //BeginKey
        bs.writeNativeVariant(0); //EndKeySize
        bs.writeInt(0); //Skip
        bs.writeInt(Integer.MAX_VALUE); //Take
        bs.writeLong(0); //Timestamp
        bs.writeByte((byte) -1); //DataCF
        bs.writeBool(false); //IsMVCC TODO: remove it
        bs.writeBool(false); //ToIndexTarget
    }
}
