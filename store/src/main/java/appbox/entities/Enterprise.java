package appbox.entities;

import appbox.data.SysEntity;
import appbox.exceptions.UnknownEntityMember;
import appbox.expressions.KVFieldExpression;
import appbox.model.entity.DataFieldModel;
import appbox.serialization.IEntityMemberReader;
import appbox.serialization.IEntityMemberWriter;
import appbox.utils.IdUtil;

public final class Enterprise extends SysEntity {
    public static final long MODELID = IdUtil.SYS_ENTERPRISE_MODEL_ID;

    public static final short NAME_ID    = (short) (1 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short ADDRESS_ID = (short) (2 << IdUtil.MEMBERID_SEQ_OFFSET);

    public static final KVFieldExpression NAME    = new KVFieldExpression(NAME_ID, DataFieldModel.DataFieldType.String);
    public static final KVFieldExpression ADDRESS = new KVFieldExpression(ADDRESS_ID, DataFieldModel.DataFieldType.String);

    private String _name;
    private String _address;

    public String getName() {
        return _name;
    }

    public void setName(String value) {
        if (!value.equals(_name)) {
            _name = value;
            onPropertyChanged(NAME_ID);
        }
    }

    public String getAddress() {
        return _address;
    }

    public void setAddress(String value) {
        if (!value.equals(_address)) {
            _address = value;
            onPropertyChanged(ADDRESS_ID);
        }
    }

    @Override
    public long modelId() {
        return MODELID;
    }

    @Override
    public void writeMember(short id, IEntityMemberWriter bs, byte flags) {
        switch (id) {
            case NAME_ID:
                bs.writeMember(id, _name, flags); break;
            case ADDRESS_ID:
                bs.writeMember(id, _address, flags); break;
            default:
                throw new UnknownEntityMember(Enterprise.class, id);
        }
    }

    @Override
    public void readMember(short id, IEntityMemberReader bs, int flags) {
        switch (id) {
            case NAME_ID:
                _name = bs.readStringMember(flags); break;
            case ADDRESS_ID:
                _address = bs.readStringMember(flags); break;
            default:
                throw new UnknownEntityMember(Enterprise.class, id);
        }
    }
}
