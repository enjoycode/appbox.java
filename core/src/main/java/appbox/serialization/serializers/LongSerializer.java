package appbox.serialization.serializers;

import appbox.serialization.*;

public final class LongSerializer extends TypeSerializer {
    public static final LongSerializer instance = new LongSerializer();

    private LongSerializer() {
        super(PayloadType.Int64, Long.class, null);
    }

    @Override
    public void write(IOutputStream bs, Object value) {
        bs.writeLong((long) value);
    }

    @Override
    public Object read(IInputStream bs, Object value) {
        return bs.readLong();
    }
}
