package appbox.store;

import appbox.data.EntityId;
import appbox.serialization.IOutputStream;

import java.nio.charset.StandardCharsets;

/**
 * 系统存储Key编码
 */
public final class KeyUtil {

    public static final long META_RAFTGROUP_ID = 0;

    public static final byte METACF_APP_PREFIX              = 0x0C;
    public static final byte METACF_MODEL_PREFIX            = 0x0D;
    public static final byte METACF_MODEL_CODE_PREFIX       = 0x0E;
    public static final byte METACF_SERVICE_ASSEMBLY_PREFIX = (byte) 0xA0;
    public static final byte METACF_VIEW_ASSEMBLY_PREFIX    = (byte) 0xA1;

    public static final byte PARTCF_INDEX             = 4;
    public static final byte PARTCF_GLOBAL_TABLE_FLAG = 0x01;
    public static final byte PARTCF_PART_TABLE_FLAG   = 0x02;
    public static final byte PARTCF_GLOBAL_INDEX_FLAG = 0x11;
    public static final byte PARTCF_PART_INDEX_FLAG   = 0x12;

    public static final byte INDEXCF_INDEX = 6;

    public static final int ENTITY_KEY_SIZE = 16;

    public static void writeAppKey(IOutputStream bs, int appId, boolean withSize) {
        if (withSize) {
            bs.writeNativeVariant(5); //注意按无符号写入key长度
        }
        bs.writeByte(KeyUtil.METACF_APP_PREFIX);
        bs.writeIntBE(appId);
    }

    public static void writeModelKey(IOutputStream bs, long modelId, boolean withSize) {
        if (withSize) {
            bs.writeNativeVariant(9); //注意按无符号写入key长度
        }
        bs.writeByte(KeyUtil.METACF_MODEL_PREFIX);
        bs.writeLongBE(modelId); //暂大字节序写入
    }

    public static void writeModelCodeKey(IOutputStream bs, long modelId, boolean withSize) {
        if (withSize) {
            bs.writeNativeVariant(9); //注意按无符号写入key长度
        }
        bs.writeByte(KeyUtil.METACF_MODEL_CODE_PREFIX);
        bs.writeLongBE(modelId);
    }

    public static void writeAssemblyKey(IOutputStream bs, boolean isService, String asmName, boolean withSize) {
        var data = asmName.getBytes(StandardCharsets.UTF_8);
        if (withSize) {
            bs.writeNativeVariant(data.length + 1);
        }

        if (isService)
            bs.writeByte(METACF_SERVICE_ASSEMBLY_PREFIX);
        else
            bs.writeByte(METACF_VIEW_ASSEMBLY_PREFIX);

        bs.write(data, 0, data.length);
    }

    public static void writeEntityKey(IOutputStream bs, EntityId id, boolean withSize) {
        //TODO: write appStoreId + tableId
        if (withSize)
            bs.writeNativeVariant(16); //注意按无符号写入key长度
        id.writeTo(bs);
    }

    public static void writeRaftGroupId(IOutputStream bs, long raftGroupId) {
        //与EntityId.initRaftGroupId一致
        //前32位
        int p1 = (int) (raftGroupId >>> 12);
        bs.writeByte((byte) (p1 & 0xFF));
        bs.writeByte((byte) (p1 >>> 8));
        bs.writeByte((byte) (p1 >>> 16));
        bs.writeByte((byte) (p1 >>> 24));
        //后12位　<< 4
        short p2 = (short) ((raftGroupId & 0xFFF) << 4);
        bs.writeByte((byte) (p2 & 0xFF));
        bs.writeByte((byte) (p2 >>> 8));
    }

    /** 合并编码AppId + 模型TableId(大字节序) */
    public static int encodeTableId(byte appId, int modelTableId) {
        int tableId = Integer.reverseBytes(modelTableId);
        return tableId | appId; //注意在低位，写入时反转
    }

}
