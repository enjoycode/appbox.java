package appbox.channel.messages;

import appbox.serialization.IOutputStream;
import appbox.store.KVUtil;

/** 用于设计时扫描加载相关模型 */
public final class KVScanModelRequest extends KVScanRequest {

    private final byte _beginKeyPrefix;
    private final byte _endKeyPrefix;

    public KVScanModelRequest(byte beginKeyPrefix, byte endKeyPrefix) {
        _beginKeyPrefix = beginKeyPrefix;
        _endKeyPrefix   = endKeyPrefix;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeInt(0); //ReqId占位
        bs.writeLong(KVUtil.META_RAFTGROUP_ID); //raftGroupId
        bs.writeNativeVariant(1); //BeginKeySize
        bs.writeByte(_beginKeyPrefix); //BeginKey
        bs.writeNativeVariant(1); //EndKeySize
        bs.writeByte(_endKeyPrefix);   //EndKey
        bs.writeInt(0); //Skip
        bs.writeInt(Integer.MAX_VALUE); //Take
        bs.writeLong(0); //Timestamp
        bs.writeByte((byte) -1); //DataCF
        bs.writeBool(false); //IsMVCC TODO: remove it
        bs.writeBool(false); //ToIndexTarget
    }

}
