package appbox.entities;

import appbox.data.SysEntity;
import appbox.exceptions.UnknownEntityMember;
import appbox.serialization.IEntityMemberReader;
import appbox.serialization.IEntityMemberWriter;
import appbox.utils.IdUtil;

public class Workgroup extends SysEntity {

    public static final short NAME_ID = (short) (1 << IdUtil.MEMBERID_SEQ_OFFSET);

    public Workgroup() {
        super(IdUtil.SYS_WORKGROUP_MODEL_ID);
    }

    private String _name;

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        if (!name.equals(_name)) {
            this._name = name;
            onPropertyChanged(NAME_ID);
        }
    }

    @Override
    public void writeMember(short id, IEntityMemberWriter bs, byte flags) {
        switch (id) {
            case NAME_ID:
                bs.writeMember(id, _name, flags); break;
            default:
                throw new UnknownEntityMember(Workgroup.class, id);
        }
    }

    @Override
    public void readMember(short id, IEntityMemberReader bs, int flags) {
        switch (id) {
            case NAME_ID:
                _name = bs.readStringMember(flags); break;
            default:
                throw new UnknownEntityMember(Workgroup.class, id);
        }
    }
}
