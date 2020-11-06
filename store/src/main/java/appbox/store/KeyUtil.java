package appbox.store;

import appbox.data.EntityId;
import appbox.serialization.BinSerializer;

/**
 * 系统存储Key编码
 */
public final class KeyUtil {

    public static final long META_RAFTGROUP_ID = 0;

    public static final byte METACF_APP_PREFIX        = 0x0C;
    public static final byte METACF_MODEL_PREFIX      = 0x0D;
    public static final byte METACF_MODEL_CODE_PREFIX = 0x0E;

    public static final byte PARTCF_INDEX             = 4;
    public static final byte PARTCF_GLOBAL_TABLE_FLAG = 0x01;
    public static final byte PARTCF_PART_TABLE_FLAG   = 0x02;
    public static final byte PARTCF_GLOBAL_INDEX_FLAG = 0x11;
    public static final byte PARTCF_PART_INDEX_FLAG   = 0x12;

    public static void writeAppKey(BinSerializer bs, int appId, boolean withSize) throws Exception {
        if (withSize) {
            bs.writeNativeVariant(5); //注意按无符号写入key长度
        }
        bs.writeByte(KeyUtil.METACF_APP_PREFIX);
        bs.writeIntBE(appId);
    }

    public static void writeModelKey(BinSerializer bs, long modelId) throws Exception {
        bs.writeNativeVariant(9); //注意按无符号写入key长度
        bs.writeByte(KeyUtil.METACF_MODEL_PREFIX);
        bs.writeLongBE(modelId); //暂大字节序写入
    }

    public static void writeModelCodeKey(BinSerializer bs, long modelId) throws Exception {
        bs.writeNativeVariant(9); //注意按无符号写入key长度
        bs.writeByte(KeyUtil.METACF_MODEL_CODE_PREFIX);
        bs.writeLongBE(modelId);
    }

    public static void writeEntityKey(BinSerializer bs, EntityId id) throws Exception {
        //TODO: write appStoreId + tableId
        id.writeTo(bs);
    }
}
