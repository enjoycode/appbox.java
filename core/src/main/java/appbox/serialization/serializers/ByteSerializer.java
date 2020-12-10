package appbox.serialization.serializers;

import appbox.serialization.BinDeserializer;
import appbox.serialization.IOutputStream;
import appbox.serialization.PayloadType;
import appbox.serialization.TypeSerializer;

public class ByteSerializer extends TypeSerializer {

    public static final ByteSerializer instance = new ByteSerializer();

    public ByteSerializer() {
        super(PayloadType.Byte, Byte.class, null);
    }

    @Override
    public void write(IOutputStream bs, Object value) {
        bs.writeByte((byte) value);
    }

    @Override
    public Object read(BinDeserializer bs, Object value) {
        return bs.readByte();
    }
}
