package appbox.channel;

import appbox.serialization.BinSerializer;
import appbox.utils.IdUtil;

public final class KVRowReader {

    private final byte[] rowData;

    public KVRowReader(byte[] value) {
        rowData = value;
    }

    /**
     * 将存储返回的成员值读出并写入
     * @param flags (1 << IdUtil.MEMBERID_ORDER_OFFSET) 或者 0
     */
    public void writeMember(short id, BinSerializer bs, byte flags) throws Exception {
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

}
