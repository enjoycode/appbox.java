package appbox.core.serialization.serializers;

import appbox.core.serialization.BinDeserializer;
import appbox.core.serialization.BinSerializer;
import appbox.core.serialization.PayloadType;
import appbox.core.serialization.TypeSerializer;

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
    public Object read(BinDeserializer bs) throws Exception {
        return bs.readString();
    }
}
