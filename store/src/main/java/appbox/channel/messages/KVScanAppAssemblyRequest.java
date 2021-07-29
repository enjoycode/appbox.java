package appbox.channel.messages;

import appbox.serialization.IOutputStream;
import appbox.store.KVUtil;
import appbox.utils.StringUtil;

/** 仅用于设计时扫描指定应用依赖的第三方库 */
public final class KVScanAppAssemblyRequest extends KVScanRequest {

    private final String appPrefix;

    public KVScanAppAssemblyRequest(String appName) {
        appPrefix = appName + "/";
    }

    @Override
    public void writeTo(IOutputStream bs) {
        int prefixSize = StringUtil.getUtf8Size(appPrefix);

        bs.writeInt(0); //ReqId占位
        bs.writeLong(KVUtil.META_RAFTGROUP_ID); //raftGroupId
        bs.writeNativeVariant(1 + prefixSize); //BeginKeySize
        bs.writeByte(KVUtil.METACF_APP_ASSEMBLY_PREFIX); //BeginKey
        bs.writeUtf8(appPrefix);
        bs.writeNativeVariant(0); //EndKeySize
        bs.writeInt(0); //Skip
        bs.writeInt(Integer.MAX_VALUE); //Take
        bs.writeLong(0); //Timestamp
        bs.writeByte((byte) -1); //DataCF
        bs.writeBool(false); //IsMVCC TODO: remove it
        bs.writeBool(false); //ToIndexTarget
    }

}
