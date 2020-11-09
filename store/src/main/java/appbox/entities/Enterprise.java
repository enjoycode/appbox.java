package appbox.entities;

import appbox.data.SysEntity;
import appbox.expressions.KVFieldExpression;
import appbox.model.entity.DataFieldModel;
import appbox.serialization.IEntityMemberReader;
import appbox.serialization.IEntityMemberWriter;
import appbox.utils.IdUtil;

public final class Enterprise extends SysEntity {
    public static final short NAME_ID    = (short) (1 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short ADDRESS_ID = (short) (2 << IdUtil.MEMBERID_SEQ_OFFSET);

    public static final KVFieldExpression NAME    = new KVFieldExpression(NAME_ID, DataFieldModel.DataFieldType.String);
    public static final KVFieldExpression ADDRESS = new KVFieldExpression(ADDRESS_ID, DataFieldModel.DataFieldType.String);

    private String _name;
    private String _address;

    public Enterprise() {
        super(IdUtil.SYS_ENTERPRISE_MODEL_ID);
    }

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
    public void writeMember(short id, IEntityMemberWriter bs, byte storeFlags) throws Exception {
        switch (id) {
            case NAME_ID:
                bs.writeMember(id, _name, storeFlags); break;
            case ADDRESS_ID:
                bs.writeMember(id, _address, storeFlags); break;
            default:
                throw new Exception("unknown member");
        }
    }

    @Override
    public void readMember(short id, IEntityMemberReader bs, int storeFlags) throws Exception {
        switch (id) {
            case NAME_ID:
                _name = bs.readStringMember(storeFlags); break;
            case ADDRESS_ID:
                _address = bs.readStringMember(storeFlags); break;
            default:
                throw new Exception("unknown member");
        }
    }
}
