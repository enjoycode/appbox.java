package appbox.store;

import appbox.serialization.BinSerializer;

/**
 * 系统存储Key编码
 */
public final class KeyUtil {

    public static final long META_RAFTGROUP_ID = 0;

    public static final byte METACF_APP_PREFIX        = 0x0C;
    public static final byte METACF_MODEL_PREFIX      = 0x0D;
    public static final byte METACF_MODEL_CODE_PREFIX = 0x0E;

    public static void writeAppKey(BinSerializer bs, int appId) throws Exception {
        bs.writeByte(KeyUtil.METACF_APP_PREFIX);
        bs.writeIntBE(appId);
    }

    public static void writeModelKey(BinSerializer bs, long modelId) throws Exception {
        bs.writeNativeVariant(9); //注意按无符号写入
        bs.writeByte(KeyUtil.METACF_MODEL_PREFIX);
        bs.writeLongBE(modelId); //暂大字节序写入
    }

    public static void writeModelCodeKey(BinSerializer bs, long modelId) throws Exception {
        bs.writeNativeVariant(9); //注意按无符号写入
        bs.writeByte(KeyUtil.METACF_MODEL_CODE_PREFIX);
        bs.writeLongBE(modelId);
    }

}
