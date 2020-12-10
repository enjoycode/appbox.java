package appbox.serialization.serializers;

import appbox.serialization.BinDeserializer;
import appbox.serialization.IOutputStream;
import appbox.serialization.PayloadType;
import appbox.serialization.TypeSerializer;

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
    public Object read(BinDeserializer bs, Object value) {
        return bs.readUUID();
    }
}
