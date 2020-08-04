package appbox.core.serialization;

import appbox.core.cache.ObjectPool;

public final class BinSerializer {
    public static final ObjectPool<BinSerializer> pool = new ObjectPool<>(BinSerializer::new, null, 32);

    private IOutputStream _stream;

    private BinSerializer() {
    }

    public void reset(IOutputStream stream) {
        _stream = stream;
    }

    public void serialize(Object obj) throws Exception {
        if (obj == null) {
            _stream.writeByte(PayloadType.Null);
            return;
        }

        var type       = obj.getClass();
        var serializer = TypeSerializer.getSerializer(type);
        if (serializer == null) {
            throw new Exception("暂未实现未知类型的反射序列化: " + type.getName());
        }

        //TODO:是否已序列化过

        //写入类型信息
        _stream.writeByte(serializer.payloadType);
        //TODO:写入附加类型信息

        //写入数据 TODO:先加入已序列化列表
        serializer.write(this, obj);
    }

    public void writeVariant(int value) {
        _stream.writeVariant(value);
    }
}
