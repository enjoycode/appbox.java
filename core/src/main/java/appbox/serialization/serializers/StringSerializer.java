package appbox.serialization.serializers;

import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.serialization.PayloadType;
import appbox.serialization.TypeSerializer;

public final class StringSerializer extends TypeSerializer {
    public static final StringSerializer instance = new StringSerializer();

    private StringSerializer() {
        super(PayloadType.String, String.class, null);
    }

    @Override
    public void write(BinSerializer bs, Object value) throws Exception {
        bs.writeString((String) value);
    }

    @Override
    public Object read(BinDeserializer bs, Object value) throws Exception {
        return bs.readString();
    }
}
