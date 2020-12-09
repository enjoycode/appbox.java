package appbox.data;

import appbox.model.EntityModel;
import appbox.model.entity.DataFieldModel;
import appbox.model.entity.EntityMemberModel;
import appbox.serialization.IEntityMemberReader;
import appbox.serialization.IEntityMemberWriter;
import appbox.serialization.IJsonSerializable;
import com.alibaba.fastjson.JSONWriter;

import java.util.HashMap;
import java.util.Map;

public final class SysEntityKVO extends SysEntity implements IJsonSerializable {

    private final Map<String, Object> _kvo = new HashMap<>(8);
    private final EntityModel _model;

    public SysEntityKVO(EntityModel model) {
        super(model.id());

        _model = model;
    }

    @Override
    public void writeMember(short id, IEntityMemberWriter bs, byte flags) {
        throw new UnsupportedOperationException("write KVO not supported.");
    }

    @Override
    public void readMember(short id, IEntityMemberReader bs, int flags) {
        var m = _model.getMember(id);
        if (m.type() == EntityMemberModel.EntityMemberType.DataField) {
            var field = (DataFieldModel) m;
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

    @Override
    public void writeToJson(JSONWriter writer) {
        writer.startObject();
        for(var entry : _kvo.entrySet()) {
            writer.writeKey(entry.getKey());
            writer.writeValue(entry.getValue()); //TODO:EntityRef & EntitySet处理
        }
        writer.endObject();
    }
}
