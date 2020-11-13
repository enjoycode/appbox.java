package appbox.utils;

import appbox.model.ModelLayer;
import appbox.model.ModelType;

public final class IdUtil {
    private IdUtil() {
    }

    public static final int RAFTGROUPID_APPID_OFFSET      = 36;
    public static final int RAFTGROUPID_FLAGS_OFFFSET     = 32;
    public static final int RAFTGROUPID_FLAGS_TYPE_OFFSET = 2;
    public static final int RAFTGROUPID_FLAGS_MVCC_OFFSET = 1;

    public static final byte RAFT_TYPE_TABLE     = 0;
    public static final byte RAFT_TYPE_INDEX     = 1;
    public static final byte RAFT_TYPE_BLOB_META = 2;
    //public static final byte RAFT_TYPE_BLOB_CHUNK = 3;

    public static final int  MODELID_APPID_OFFSET = 32;
    public static final int  MODELID_TYPE_OFFSET  = 24;
    public static final int  MODELID_SEQ_OFFSET   = 2;
    public static final long MODELID_LAYER_MASK   = 3;

    public static final int INDEXID_UNIQUE_OFFSET = 7;

    public static final short MEMBERID_MASK         = (short) 0xFFE0; //2的11次方左移5位
    public static final short MEMBERID_LENFLAG_MASK = 0xF; //后4位
    public static final int   MEMBERID_SEQ_OFFSET   = 7;
    public static final int   MEMBERID_LAYER_OFFSET = 5;
    public static final int   MEMBERID_ORDER_OFFSET = 4;

    public static final byte  STORE_FIELD_VAR_FLAG        = 0;
    public static final byte  STORE_FIELD_BOOL_TRUE_FLAG  = 3;
    public static final byte  STORE_FIELD_BOOL_FALSE_FLAG = 5;
    public static final byte  STORE_FIELD_16_LEN_FLAG     = 7;
    public static final byte  STORE_FIELD_NULL_FLAG       = 9;
    public static final short STORE_FIELD_ID_OF_ENTITY_ID = 7; //用于存储索引指向的实体的Id, 相当于MemberId(0) | 16LenFlag

    public static final int  SYS_APP_ID              = 0x9E9AA8F7;
    public static final long SYS_ENTITY_MODEL_ID     = Integer.toUnsignedLong(SYS_APP_ID) << MODELID_APPID_OFFSET
            | Byte.toUnsignedLong(ModelType.Entity.value) << MODELID_TYPE_OFFSET;
    public static final long SYS_EMPLOYEE_MODEL_ID    = SYS_ENTITY_MODEL_ID | (1 << MODELID_SEQ_OFFSET);
    public static final long SYS_ENTERPRISE_MODEL_ID = SYS_ENTITY_MODEL_ID | (2 << MODELID_SEQ_OFFSET);
    public static final long SYS_WORKGROUP_MODEL_ID  = SYS_ENTITY_MODEL_ID | (3 << MODELID_SEQ_OFFSET);
    public static final long SYS_ORGUNIT_MODEL_ID    = SYS_ENTITY_MODEL_ID | (4 << MODELID_SEQ_OFFSET);
    public static final long SYS_STAGED_MODEL_ID     = SYS_ENTITY_MODEL_ID | (5 << MODELID_SEQ_OFFSET);
    public static final long SYS_CHECKOUT_MODEL_ID   = SYS_ENTITY_MODEL_ID | (6 << MODELID_SEQ_OFFSET);

    public static int getAppIdFromModelId(long modelId) {
        return (int) (modelId >>> MODELID_APPID_OFFSET);
    }

    public static ModelType getModelTypeFromModelId(long modelId) {
        byte value = (byte) ((modelId >> MODELID_TYPE_OFFSET) & 0xFF);
        return ModelType.fromValue(value);
    }

    /**
     * 根据模型层级及成员流水号生成实体成员标识
     */
    public static short makeMemberId(ModelLayer layer, int seq) {
        return (short) (seq << IdUtil.MEMBERID_SEQ_OFFSET
                | Byte.toUnsignedInt(layer.value) << IdUtil.MEMBERID_LAYER_OFFSET);
    }


}
