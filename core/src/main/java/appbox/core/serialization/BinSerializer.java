package appbox.core.serialization;

import appbox.core.cache.ObjectPool;

public final class BinSerializer {
    private static final ObjectPool<BinSerializer> pool = new ObjectPool<>(BinSerializer::new, 32);

    public static BinSerializer rentFromPool(IOutputStream stream) {
        var obj = pool.rent();
        obj._stream = stream;
        return obj;
    }

    public static void backToPool(BinSerializer obj) {
        obj._stream = null;
        pool.back(obj);
    }

    private IOutputStream _stream;

    private BinSerializer() {
    }

    public void serialize(Object obj) throws Exception {
        if (obj == null) {
            _stream.writeByte(PayloadType.Null);
            return;
        } else if (obj == Boolean.TRUE) {
            _stream.writeByte(PayloadType.BooleanTrue);
            return;
        } else if (obj == Boolean.FALSE) {
            _stream.writeByte(PayloadType.BooleanFalse);
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

    public void writeByte(byte value) throws Exception {
        _stream.writeByte(value);
    }

    public void writeShort(short value) throws Exception {
        _stream.writeShort(value);
    }

    public void writeInt(int value) throws Exception {
        _stream.writeInt(value);
    }

    public void writeInt(int value, int fieldId) throws Exception {
        _stream.writeVariant(fieldId);
        _stream.writeInt(value);
    }

    public void writeVariant(int value) {
        _stream.writeVariant(value);
    }

    public void writeVariant(int value, int fieldId) throws Exception {
        _stream.writeVariant(fieldId);
        _stream.writeVariant(value);
    }

    public void writeString(String value) throws Exception {
        _stream.writeString(value);
    }

    public void writeString(String value, int fieldId) throws Exception {
        _stream.writeVariant(fieldId);
        _stream.writeString(value);
    }

    public void finishWriteFields() throws Exception {
        _stream.writeVariant(0);
    }
}
