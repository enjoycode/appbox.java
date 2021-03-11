package appbox.store;

import appbox.channel.MemberSizeCounter;
import appbox.data.EntityId;
import appbox.data.SysEntity;
import appbox.model.ModelType;
import appbox.model.entity.EntityRefModel;
import appbox.model.entity.SysIndexModel;
import appbox.serialization.IEntityMemberWriter;
import appbox.serialization.IOutputStream;
import appbox.utils.IdUtil;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** 系统存储编码 */
public final class KVUtil {

    public static final long META_RAFTGROUP_ID = 0;

    public static final byte METACF_APP_PREFIX              = (byte) 0xA0;
    public static final byte METACF_FOLDER_PREFIX           = (byte) 0xA1;
    public static final byte METACF_BLOB_PREFIX             = (byte) 0xA2;
    public static final byte METACF_DATASTORE_PREFIX        = (byte) 0xA3;
    public static final byte METACF_MODEL_PREFIX            = (byte) 0xA4;
    public static final byte METACF_MODEL_CODE_PREFIX       = (byte) 0xC0;
    public static final byte METACF_SERVICE_ASSEMBLY_PREFIX = (byte) 0xB0;
    public static final byte METACF_VIEW_ASSEMBLY_PREFIX    = (byte) 0xB1;
    public static final byte METACF_VIEW_ROUTE_PREFIX       = (byte) 0xB2;

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
        bs.writeByte(KVUtil.METACF_APP_PREFIX);
        bs.writeIntBE(appId);
    }

    public static void writeBlobStoreKey(IOutputStream bs, String storeName) {
        bs.writeByte(KVUtil.METACF_BLOB_PREFIX);
        bs.writeUtf8(storeName);
    }

    public static void writeDataStoreKey(IOutputStream bs, long storeId, boolean withSize) {
        if (withSize) {
            bs.writeNativeVariant(9); //注意按无符号写入key长度
        }
        bs.writeByte(KVUtil.METACF_DATASTORE_PREFIX);
        bs.writeLongBE(storeId); //暂大字节序写入
    }

    public static void writeModelKey(IOutputStream bs, long modelId, boolean withSize) {
        if (withSize) {
            bs.writeNativeVariant(9); //注意按无符号写入key长度
        }
        bs.writeByte(KVUtil.METACF_MODEL_PREFIX);
        bs.writeLongBE(modelId); //暂大字节序写入
    }

    public static void writeModelCodeKey(IOutputStream bs, long modelId, boolean withSize) {
        if (withSize) {
            bs.writeNativeVariant(9); //注意按无符号写入key长度
        }
        bs.writeByte(KVUtil.METACF_MODEL_CODE_PREFIX);
        bs.writeLongBE(modelId);
    }

    public static void writeFolderKey(IOutputStream bs, int appId, ModelType modelType, boolean withSize) {
        if (withSize) {
            bs.writeNativeVariant(6);
        }
        bs.writeByte(KVUtil.METACF_FOLDER_PREFIX);
        bs.writeIntBE(appId);
        bs.writeByte(modelType.value);
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

    public static void writeEntityRefs(IOutputStream bs, List<EntityRefModel> refsWithFK) {
        //暂存储层是字符串，实际是short数组
        int refsSize = 0;
        if (refsWithFK != null && refsWithFK.size() > 0) {
            refsSize = refsWithFK.size() * 2;
        }
        bs.writeNativeVariant(refsSize);
        if (refsSize > 0) {
            for (var r : refsWithFK) {
                bs.writeShort(r.getFKMemberIds()[0]);
            }
        }
    }

    public static void writeIndexKey(IOutputStream bs, SysIndexModel indexModel, SysEntity entity, boolean withSize) {
        if (withSize) {
            //先计算并写入Key长度
            var sizeCounter = new MemberSizeCounter();
            for (var field : indexModel.fields()) {
                entity.writeMember(field.memberId, sizeCounter, IEntityMemberWriter.SF_NONE); //flags无意义
            }
            bs.writeNativeVariant(6 + 1 + sizeCounter.getSize());
        }

        //写入EntityId's RaftGroupId
        entity.id().writePart1(bs);
        //写入IndexId
        bs.writeByte(indexModel.indexId());
        //写入各字段, 注意MemberId写入排序标记
        for (var field : indexModel.fields()) {
            //惟一索引但字段不具备值的处理, 暂 id | null flag
            byte flags = IEntityMemberWriter.SF_STORE | IEntityMemberWriter.SF_WRITE_NULL;
            if (field.orderByDesc)
                flags |= IEntityMemberWriter.SF_ORDER_BY_DESC;
            entity.writeMember(field.memberId, bs, flags);
        }
        //写入非惟一索引的EntityId的第二部分
        if (!indexModel.unique()) {
            entity.id().writePart2(bs);
        }
    }

    public static void writeIndexValue(IOutputStream bs, SysIndexModel indexModel, SysEntity entity) {
        //先写入惟一索引指向的EntityId
        if (indexModel.unique()) {
            bs.writeShort(IdUtil.STORE_FIELD_ID_OF_ENTITY_ID);
            entity.id().writeTo(bs);
        }
        //再写入StoringFields
        if (indexModel.hasStoringFields()) {
            for (var sf : indexModel.storingFields()) {
                entity.writeMember(sf, bs, IEntityMemberWriter.SF_STORE); //暂不写入null成员
            }
        }
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
