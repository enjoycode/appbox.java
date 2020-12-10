package appbox.runtime;

import appbox.cache.ObjectPool;
import appbox.serialization.BinDeserializer;
import appbox.serialization.IOutputStream;
import appbox.serialization.PayloadType;

/**
 * 调用服务的参数
 */
public final class InvokeArg {
    public static final ObjectPool<InvokeArg> pool = new ObjectPool<>(InvokeArg::new, 32);

    //region ====static helpers====
    public static InvokeArg from(String v) {
        var arg = pool.rent();
        arg.setValue(v);
        return arg;
    }

    public static InvokeArg from(int v) {
        var arg = pool.rent();
        arg.setValue(v);
        return arg;
    }
    //endregion

    private Object value;
    private byte   type; //常用类型简化

    private InvokeArg() {
    }

    public void setValue(int v) {
        type  = PayloadType.Int32;
        value = v;
    }

    public void setValue(String v) {
        type = PayloadType.String;
        value = v;
    }

    public void setValue(Object v) {
        type  = PayloadType.UnknownType;
        value = v;
    }

    public short getShort() {
        return (short) value;
    }

    public int getInt() {
        return (int) value;
    }

    public long getLong() {
        return (long) value;
    }

    public String getString() {
        return (String) value;
    }

    public void writeTo(IOutputStream bs) {
        //TODO:暂简单实现，待优化
        bs.serialize(value);
    }

    public void readFrom(BinDeserializer bs) {
        //TODO:暂简单实现，待优化
        value = bs.deserialize();
    }
}
