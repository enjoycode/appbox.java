package appbox.core.data;

import appbox.core.cache.ObjectPool;
import appbox.core.serialization.BinSerializer;
import appbox.core.serialization.PayloadType;

/**
 * 调用服务的参数 TODO:暂简单实现
 */
public final class InvokeArg {
    public static final ObjectPool<InvokeArg> pool = new ObjectPool<>(InvokeArg::new, null, 32);

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

    public void writeTo(BinSerializer bs) throws Exception {

    }
}
