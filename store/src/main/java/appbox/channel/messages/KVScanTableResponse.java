package appbox.channel.messages;

import appbox.channel.KVRowReader;
import appbox.data.SysEntity;
import appbox.serialization.BinDeserializer;
import appbox.store.KeyUtil;

import java.util.ArrayList;
import java.util.List;

public final class KVScanTableResponse<T extends SysEntity> extends KVScanResponse {

    public        List<T>  result;
    private final Class<T> clazz;

    public KVScanTableResponse(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public void readFrom(BinDeserializer bs) {
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
                T obj = null;
                try {
                    obj = clazz.getDeclaredConstructor().newInstance();
                } catch (Exception ex) {
                    throw new RuntimeException("Can't create instance.");
                }
                result.add(obj);
                obj.id().readFrom(bs);
                //开始读取当前行的各个字段
                KVRowReader.readFields(bs, obj);
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
