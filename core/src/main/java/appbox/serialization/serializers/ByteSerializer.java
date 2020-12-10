package appbox.serialization.serializers;

import appbox.serialization.*;

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
    public Object read(IInputStream bs, Object value) {
        return bs.readByte();
    }
}
