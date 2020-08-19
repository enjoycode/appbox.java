package appbox.serialization.serializers;

import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.serialization.PayloadType;
import appbox.serialization.TypeSerializer;

public final class IntSerializer extends TypeSerializer {
    public static final IntSerializer instance = new IntSerializer();

    private IntSerializer() {
        super(PayloadType.Int32, Integer.class, null);
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
