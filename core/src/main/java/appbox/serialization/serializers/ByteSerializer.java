package appbox.serialization.serializers;

import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.serialization.PayloadType;
import appbox.serialization.TypeSerializer;

public class ByteSerializer extends TypeSerializer {

    public static final ByteSerializer instance = new ByteSerializer();

    public ByteSerializer() {
        super(PayloadType.Byte, Byte.class, null);
    }

    @Override
    public void write(BinSerializer bs, Object value) {
        bs.write((byte)value);
    }

    @Override
    public Object read(BinDeserializer bs, Object value) {
        return bs.readByte();
    }
}
