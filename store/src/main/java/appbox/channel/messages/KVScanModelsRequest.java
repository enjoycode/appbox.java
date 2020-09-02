package appbox.channel.messages;

import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.store.KeyUtil;

/**
 * 扫描所有模型，用于设计时加载
 */
public final class KVScanModelsRequest extends KVScanRequest {
    public enum ModelsType {
        Applications, Models;
    }

    private final ModelsType scanWhat;

    public KVScanModelsRequest(ModelsType scanWhat) {
        this.scanWhat = scanWhat;
    }

    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        bs.writeInt(0); //ReqId占位
        bs.writeLong(KeyUtil.META_RAFTGROUP_ID); //raftGroupId
        bs.writeNativeVariant(1); //BeginKeySize
        if (scanWhat == ModelsType.Applications) {
            bs.writeByte(KeyUtil.METACF_APP_PREFIX); //BeginKey
        } else {
            bs.writeByte(KeyUtil.METACF_MODEL_PREFIX); //BeginKey
        }
        bs.writeNativeVariant(0); //EndKeySize
        bs.writeInt(0); //Skip
        bs.writeInt(Integer.MAX_VALUE); //Take
        bs.writeLong(0); //Timestamp
        bs.writeByte((byte) -1); //DataCF

        if (scanWhat == ModelsType.Applications) {
            bs.writeByte((byte) 1); //DataType
        } else {
            bs.writeByte((byte) 2); //DataType
        }

        bs.writeBool(false); //IsMVCC TODO: remove it
        bs.writeBool(false); //ToIndexTarget
        bs.writeBool(false); //HasFilter
    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {
        throw new Exception("Not supported.");
    }
}
