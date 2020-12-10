package appbox.serialization.serializers;

import appbox.serialization.BinDeserializer;
import appbox.serialization.IOutputStream;
import appbox.serialization.PayloadType;
import appbox.serialization.TypeSerializer;

public final class StringSerializer extends TypeSerializer {
    public static final StringSerializer instance = new StringSerializer();

    private StringSerializer() {
        super(PayloadType.String, String.class, null);
    }

    @Override
    public void write(IOutputStream bs, Object value) {
        bs.writeString((String) value);
    }

    @Override
    public Object read(BinDeserializer bs, Object value) {
        return bs.readString();
    }
}
