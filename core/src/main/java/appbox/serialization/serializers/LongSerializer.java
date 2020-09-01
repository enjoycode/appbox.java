package appbox.serialization.serializers;

import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.serialization.PayloadType;
import appbox.serialization.TypeSerializer;

public final class LongSerializer extends TypeSerializer {
    public static final LongSerializer instance = new LongSerializer();

    private LongSerializer() {
        super(PayloadType.Int64, Long.class, null);
    }

    @Override
    public void write(BinSerializer bs, Object value) throws Exception {
        bs.writeLong((long) value);
    }

    @Override
    public Object read(BinDeserializer bs, Object value) throws Exception {
        return bs.readLong();
    }
}
