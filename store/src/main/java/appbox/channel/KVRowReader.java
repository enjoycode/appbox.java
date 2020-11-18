package appbox.channel;

import appbox.data.EntityId;
import appbox.data.IKVRow;
import appbox.data.SysIndex;
import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.utils.IdUtil;

public final class KVRowReader {

    private KVRowReader() {}

    /**
     * 将存储返回的成员值读出并写入，主要用于Update or Delete entity后更新索引
     * @param flags (1 << IdUtil.MEMBERID_ORDER_OFFSET) 或者 0
     */
    public static void writeMember(byte[] rowData, short id, BinSerializer bs, byte flags) {
        int   cur       = 0;
        short fieldId   = 0;
        int   fieldSize = 0; //包含可变长度信息
        byte  lenFlag   = 0;

        while (cur < rowData.length) {
            fieldId = (short) (rowData[cur] | (rowData[cur + 1] << 8));
            lenFlag = (byte) (fieldId & IdUtil.MEMBERID_LENFLAG_MASK);
            switch (lenFlag) {
                case IdUtil.STORE_FIELD_VAR_FLAG:
                    fieldSize = rowData[cur + 2] | (rowData[cur + 3] << 8) | (rowData[cur + 4] << 16);
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
                bs.writeShort((short) (fieldId | flags));   //重新写入带排序标记的memberId
                bs.write(rowData, cur + 2, fieldSize);
                return;
            }

            cur += fieldSize + 2;
        }

        //here not found
        bs.writeShort((short) (id | flags | IdUtil.STORE_FIELD_NULL_FLAG));   //重新写入带排序标记的memberId
    }

    /** 专用于读取从存储返回的行数据 */
    public static void readFields(BinDeserializer bs, IKVRow target) {
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
                        var targetEntityId = new EntityId();
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

}
