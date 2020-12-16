package appbox.data;

import appbox.model.EntityModel;
import appbox.model.entity.DataFieldModel;
import appbox.model.entity.EntityMemberModel;
import appbox.serialization.IEntityMemberReader;
import appbox.serialization.IJsonWriter;

import java.util.HashMap;
import java.util.Map;

final class EntityKVO {

    private final Map<String, Object> _kvo = new HashMap<>(8);
    private final EntityModel         _model;

    public EntityKVO(EntityModel model) {
        _model = model;
    }

    void readMember(short id, IEntityMemberReader bs, int flags) {
        var m = _model.getMember(id);
        if (m.type() == EntityMemberModel.EntityMemberType.DataField) {
            var    field      = (DataFieldModel) m;
            Object fieldValue = null;
            switch (field.dataType()) {
                case EntityId:
                    fieldValue = bs.readEntityIdMember(flags);
                    break;
                case String:
                    fieldValue = bs.readStringMember(flags);
                    break;
                case DateTime:
                    fieldValue = bs.readDateMember(flags);
                    break;
                case Short:
                    throw new RuntimeException("未实现");
                case Enum:
                case Int:
                    fieldValue = bs.readIntMember(flags);
                    break;
                case Long:
                    fieldValue = bs.readLongMember(flags);
                    break;
                case Decimal:
                    throw new RuntimeException("未实现");
                case Bool:
                    fieldValue = bs.readBoolMember(flags);
                    break;
                case Guid:
                    fieldValue = bs.readUUIDMember(flags);
                    break;
                case Byte:
                    fieldValue = bs.readByteMember(flags);
                    break;
                case Binary:
                    fieldValue = bs.readBinaryMember(flags);
                    break;
                case Float:
                    throw new RuntimeException("未实现");
                case Double:
                    throw new RuntimeException("未实现");
            }

            _kvo.put(m.name(), fieldValue);
        } else {
            //TODO:
        }
    }

    void writeToJson(IJsonWriter writer) {
        writer.startObject();
        for (var entry : _kvo.entrySet()) {
            writer.writeKey(entry.getKey());
            writer.writeValue(entry.getValue()); //TODO:EntityRef & EntitySet处理
        }
        writer.endObject();
    }

}
