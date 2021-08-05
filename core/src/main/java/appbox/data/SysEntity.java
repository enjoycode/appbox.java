package appbox.data;

import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;

public abstract class SysEntity extends DBEntity implements IKVRow {
    private final EntityId _id = new EntityId();

    public final EntityId id() {
        return _id;
    }

    @Override
    public final void writeTo(IOutputStream bs) {
        super.writeTo(bs);

        _id.writeTo(bs);
    }

    @Override
    public final void readFrom(IInputStream bs) {
        super.readFrom(bs);

        _id.readFrom(bs);
    }
}
