package appbox.channel.messages;

import appbox.serialization.BinSerializer;
import appbox.store.KeyUtil;

public final class KVScanAppsRequest extends KVScanRequest {
    @Override
    public void writeTo(BinSerializer bs) {
        bs.writeInt(0); //ReqId占位
        bs.writeLong(KeyUtil.META_RAFTGROUP_ID); //raftGroupId
        bs.writeNativeVariant(1); //BeginKeySize
        bs.writeByte(KeyUtil.METACF_APP_PREFIX); //BeginKey
        bs.writeNativeVariant(0); //EndKeySize
        bs.writeInt(0); //Skip
        bs.writeInt(Integer.MAX_VALUE); //Take
        bs.writeLong(0); //Timestamp
        bs.writeByte((byte) -1); //DataCF
        bs.writeBool(false); //IsMVCC TODO: remove it
        bs.writeBool(false); //ToIndexTarget
        bs.writeBool(false); //HasFilter
    }
}
