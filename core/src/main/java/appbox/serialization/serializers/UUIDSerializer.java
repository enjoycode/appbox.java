package appbox.serialization.serializers;

import appbox.serialization.*;

import java.util.UUID;

public class UUIDSerializer extends TypeSerializer {

    public static final UUIDSerializer instance = new UUIDSerializer();

    public UUIDSerializer() {
        super(PayloadType.Guid, UUID.class, null);
    }

    @Override
    public void write(IOutputStream bs, Object value) {
        bs.writeUUID((UUID) value);
    }

    @Override
    public Object read(IInputStream bs, Object value) {
        return bs.readUUID();
    }
}
