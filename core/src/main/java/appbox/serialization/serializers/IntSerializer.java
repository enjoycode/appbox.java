package appbox.serialization.serializers;

import appbox.serialization.*;

public final class IntSerializer extends TypeSerializer {
    public static final IntSerializer instance = new IntSerializer();

    private IntSerializer() {
        super(PayloadType.Int32, Integer.class, null);
    }

    @Override
    public void write(IOutputStream bs, Object value) {
        bs.writeInt((int) value);
    }

    @Override
    public Object read(IInputStream bs, Object value) {
        return bs.readInt();
    }
}
