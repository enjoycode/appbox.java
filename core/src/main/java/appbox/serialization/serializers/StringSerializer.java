package appbox.serialization.serializers;

import appbox.serialization.*;

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
    public Object read(IInputStream bs, Object value) {
        return bs.readString();
    }
}
