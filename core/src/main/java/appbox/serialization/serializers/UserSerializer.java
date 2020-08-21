package appbox.serialization.serializers;

import appbox.serialization.*;

import java.util.function.Supplier;

/**
 * 实现了IBinSerializable的类型的默认序列化实现
 */
public final class UserSerializer extends TypeSerializer { //TODO: rename to KnownTypeSerializer
    public UserSerializer(byte payloadType, Class<?> targetType, Supplier<Object> creator) {
        super(payloadType, targetType, creator);
    }

    @Override
    public void write(BinSerializer bs, Object value) throws Exception {
        ((IBinSerializable) value).writeTo(bs);
    }

    @Override
    public Object read(BinDeserializer bs, Object value) throws Exception {
        ((IBinSerializable) value).readFrom(bs);
        return value;
    }
}
