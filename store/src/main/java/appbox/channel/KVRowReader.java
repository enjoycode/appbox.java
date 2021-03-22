package appbox.channel;

import appbox.data.EntityId;
import appbox.data.IKVRow;
import appbox.data.SysEntity;
import appbox.data.SysIndex;
import appbox.serialization.BytesOutputStream;
import appbox.serialization.IEntityMemberWriter;
import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;
import appbox.utils.IdUtil;

import java.util.Arrays;

/** 用于读写存储层返回的原始数据 */
public final class KVRowReader {

    private static class MemberPosition {
        public final int index;
        public final int size;

        public MemberPosition(int index, int size) {
            this.index = index;
            this.size  = size;
        }
    }

    private KVRowReader() {}

    /** 从存储原始数据中找出指定成员的位置及大小(包括可变长度的3字节长度) */
    private static MemberPosition findMember(byte[] rowData, short id) {
        int   cur       = 0;
        short fieldId   = 0;
        int   fieldSize = 0; //包含可变长度信息
        byte  lenFlag   = 0;

        while (cur < rowData.length) {
            fieldId = (short) (Byte.toUnsignedInt(rowData[cur]) | (Byte.toUnsignedInt(rowData[cur + 1]) << 8));
            lenFlag = (byte) (fieldId & IdUtil.MEMBERID_LENFLAG_MASK);
            switch (lenFlag) {
                case IdUtil.STORE_FIELD_VAR_FLAG:
                    fieldSize = Byte.toUnsignedInt(rowData[cur + 2])
                            | (Byte.toUnsignedInt(rowData[cur + 3]) << 8)
                            | (Byte.toUnsignedInt(rowData[cur + 4]) << 16);
                    fieldSize += 3;
                    break;
                case IdUtil.STORE_FIELD_BOOL_TRUE_FLAG:
                case IdUtil.STORE_FIELD_BOOL_FALSE_FLAG:
                case IdUtil.STORE_FIELD_NULL_FLAG:
                    fieldSize = 0;
                    break;
                case IdUtil.STORE_FIELD_16_LEN_FLAG:
                    fieldSize = 16;
                    break;
                default:
                    fieldSize = lenFlag;
                    break;
            }

            if ((fieldId & IdUtil.MEMBERID_MASK) == id) {
                return new MemberPosition(cur, fieldSize);
            }

            cur += fieldSize + 2;
        }

        //here not found
        return null;
    }

    /** 从原始数据中读取指定成员的EntityId，不存在返回null */
    public static EntityId readEntityId(byte[] rowData, short id) {
        var pos = findMember(rowData, id);
        if (pos == null)
            return null;
        if (pos.size != 16 && pos.size != 0)
            throw new RuntimeException("Invalidate EntityId size");

        return new EntityId(rowData, pos.index + 2);
    }

    /**
     * 将存储返回的成员值读出并写入，主要用于Update or Delete entity后更新索引
     * @param bs
     * @param flags (1 << IdUtil.MEMBERID_ORDER_OFFSET) 或者 0
     */
    public static void writeMember(byte[] rowData, short id, IOutputStream bs, byte flags) {
        var pos = findMember(rowData, id);
        if (pos == null) {
            bs.writeShort((short) (id | flags | IdUtil.STORE_FIELD_NULL_FLAG));   //重新写入带排序标记的memberId
        } else {
            var fieldId = (short) (rowData[pos.index] | (rowData[pos.index + 1] << 8));
            bs.writeShort((short) (fieldId | flags));   //重新写入带排序标记的memberId
            bs.write(rowData, pos.index + 2, pos.size);
        }
    }

    /** 专用于读取从存储返回的行数据 */
    public static void readFields(IInputStream bs, IKVRow target) {
        var   valueSize = bs.readNativeVariant(); //Row's value size
        int   readSize  = 0;
        short fieldId   = 0; //存储的字段标识
        short memberId  = 0; //转换后的成员标识
        byte  dataLenFlag;
        while (readSize < valueSize) {
            //读取Id及LenFlag
            fieldId     = bs.readShort();
            readSize += 2;
            memberId    = (short) (fieldId & IdUtil.MEMBERID_MASK); //由存储格式转换
            dataLenFlag = (byte) (fieldId & IdUtil.MEMBERID_LENFLAG_MASK);
            //根据标志进行相应的读取，另null值不需要处理
            switch (dataLenFlag) {
                case IdUtil.STORE_FIELD_VAR_FLAG:
                    int varSize = bs.readStoreVarLen();
                    if (varSize < 0)
                        throw new RuntimeException("store varsize < 0");
                    target.readMember(memberId, bs, (varSize << 8) | 1);
                    readSize += varSize + 3;
                    break;
                case IdUtil.STORE_FIELD_BOOL_TRUE_FLAG:
                case IdUtil.STORE_FIELD_BOOL_FALSE_FLAG:
                    target.readMember(memberId, bs, dataLenFlag);
                    break;
                case IdUtil.STORE_FIELD_16_LEN_FLAG:
                    //注意读惟一索引指向的目标时，memberId == 0
                    if (memberId == 0) {
                        var targetEntityId = EntityId.empty();
                        targetEntityId.readFrom(bs);
                        ((SysIndex<?>) target).setTargetId(targetEntityId);
                    } else {
                        target.readMember(memberId, bs, IdUtil.STORE_FIELD_16_LEN_FLAG);
                    }
                    readSize += 16;
                    break;
                case IdUtil.STORE_FIELD_NULL_FLAG: //null不需要读取
                    break;
                default:
                    target.readMember(memberId, bs, dataLenFlag);
                    readSize += dataLenFlag;
                    break;
            }
        }
    }

    /** 判断单个原始字段的值是否等于实体的属性 */
    public static boolean isFieldSameTo(byte[] rowData, SysEntity entity, short id, BytesOutputStream tempStream) {
        entity.writeMember(id, tempStream, IEntityMemberWriter.SF_STORE);
        var pos = findMember(rowData, id);
        if (pos == null)
            return tempStream.size() == 0;
        if (tempStream.size() != pos.size + 2)
            return false;
        return Arrays.equals(tempStream.getBuffer(), 0, tempStream.size()
                , rowData, pos.index, pos.index + pos.size + 2);
    }

}
