package appbox.core.data;

import appbox.core.cache.ObjectPool;
import appbox.core.serialization.BinDeserializer;
import appbox.core.serialization.BinSerializer;
import appbox.core.serialization.PayloadType;

/**
 * 调用服务的参数
 */
public final class InvokeArg {
    public static final ObjectPool<InvokeArg> pool = new ObjectPool<>(InvokeArg::new, 32);

    private Object value;
    private byte   type; //常用类型简化

    private InvokeArg() {
    }

    public void setValue(int v) {
        type  = PayloadType.Int32;
        value = v;
    }

    public void setValue(Object v) {
        type  = PayloadType.UnknownType;
        value = v;
    }

    public int getInt() {
        return (int) value;
    }

    public void writeTo(BinSerializer bs) throws Exception {
        //TODO:暂简单实现，待优化
        bs.serialize(value);
    }

    public void readFrom(BinDeserializer bs) throws Exception {
        //TODO:暂简单实现，待优化
        value = bs.deserialize();
    }
}
