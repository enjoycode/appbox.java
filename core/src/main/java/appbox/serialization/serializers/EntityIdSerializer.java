package appbox.serialization.serializers;

import appbox.data.EntityId;
import appbox.serialization.*;

public class EntityIdSerializer extends TypeSerializer {

    public static final EntityIdSerializer instance = new EntityIdSerializer();

    public EntityIdSerializer() {
        super(PayloadType.EntityId, EntityId.class, null);
    }

    @Override
    public void write(IOutputStream bs, Object value) {
        ((EntityId) value).writeTo(bs);
    }

    @Override
    public Object read(IInputStream bs, Object value) {
        ((EntityId) value).readFrom(bs);
        return value;
    }
}
