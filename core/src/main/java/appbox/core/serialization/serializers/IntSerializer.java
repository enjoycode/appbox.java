package appbox.core.serialization.serializers;

import appbox.core.serialization.BinDeserializer;
import appbox.core.serialization.BinSerializer;
import appbox.core.serialization.PayloadType;
import appbox.core.serialization.TypeSerializer;

public final class IntSerializer extends TypeSerializer {
    public static final IntSerializer instance = new IntSerializer(PayloadType.Int32, Integer.class);

    public IntSerializer(byte payloadType, Class<?> targetType) {
        super(payloadType, targetType, null);
    }

    @Override
    public void write(BinSerializer bs, Object value) throws Exception {
        bs.writeVariant((int) value);
    }

    @Override
    public Object read(BinDeserializer bs) throws Exception {
        return bs.readVariant();
    }
}
