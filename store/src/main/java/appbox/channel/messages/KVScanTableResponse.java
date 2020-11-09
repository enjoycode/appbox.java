package appbox.channel.messages;

import appbox.data.SysEntity;
import appbox.serialization.BinDeserializer;
import appbox.store.KeyUtil;
import appbox.utils.IdUtil;

import java.util.ArrayList;
import java.util.List;

public final class KVScanTableResponse<T extends SysEntity> extends KVScanResponse {

    public  List<T>  result;
    private Class<T> clazz;

    public KVScanTableResponse(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {
        reqId     = bs.readInt();
        errorCode = bs.readInt();

        if (errorCode == 0) {
            skipped = bs.readInt();
            length  = bs.readInt();

            result = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                var keySize = bs.readNativeVariant(); //Row's key size
                assert keySize == KeyUtil.ENTITY_KEY_SIZE;
                //创建对象实例并从RowKey读取Id
                var obj = clazz.getDeclaredConstructor().newInstance();
                result.add(obj);
                obj.id().readFrom(bs);
                //开始读取当前行的各个字段
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
                            obj.readMember(memberId, bs, (varSize << 8) | 1);
                            readSize += varSize + 3;
                            break;
                        case IdUtil.STORE_FIELD_BOOL_TRUE_FLAG:
                            obj.readMember(memberId, bs, IdUtil.STORE_FIELD_BOOL_TRUE_FLAG);
                            break;
                        case IdUtil.STORE_FIELD_BOOL_FALSE_FLAG:
                            obj.readMember(memberId, bs, IdUtil.STORE_FIELD_BOOL_FALSE_FLAG);
                            break;
                        case IdUtil.STORE_FIELD_16_LEN_FLAG:
                            obj.readMember(memberId, bs, IdUtil.STORE_FIELD_16_LEN_FLAG);
                            readSize += 16;
                            break;
                        case IdUtil.STORE_FIELD_NULL_FLAG: //null不需要读取
                            break;
                        default:
                            obj.readMember(memberId, bs, dataLenFlag);
                            readSize += dataLenFlag;
                            break;
                    }
                }
            }
        }
    }

    //private T makeInstance() throws Exception {
    //    if (clazz == null) {
    //        var testType = ReflectUtil.getRawType(this.getClass());
    //
    //        Type superClass = getClass().getGenericSuperclass();
    //        Type type       = ((ParameterizedType) superClass).getActualTypeArguments()[0];
    //        clazz = ReflectUtil.getRawType(type);
    //    }
    //    return (T) clazz.getDeclaredConstructor().newInstance();
    //}
}
