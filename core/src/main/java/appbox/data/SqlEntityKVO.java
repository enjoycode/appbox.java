package appbox.data;

import appbox.model.EntityModel;
import appbox.serialization.IEntityMemberReader;
import appbox.serialization.IEntityMemberWriter;
import appbox.serialization.IJsonSerializable;
import appbox.serialization.IJsonWriter;

public final class SqlEntityKVO extends SqlEntity implements IJsonSerializable {

    private final EntityKVO kvo;

    public SqlEntityKVO(EntityModel model) {
        super(model.id());

        kvo = new EntityKVO(model);
    }

    @Override
    public void writeMember(short id, IEntityMemberWriter bs, byte flags) {
        throw new UnsupportedOperationException("write KVO not supported.");
    }

    @Override
    public void readMember(short id, IEntityMemberReader bs, int flags) {
        kvo.readMember(id, bs, flags);
    }

    @Override
    public void writeToJson(IJsonWriter writer) {
        kvo.writeToJson(writer);
    }

}
