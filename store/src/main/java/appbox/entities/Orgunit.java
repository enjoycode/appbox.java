package appbox.entities;

import appbox.data.SysEntity;
import appbox.serialization.IEntityMemberReader;
import appbox.serialization.IEntityMemberWriter;
import appbox.utils.IdUtil;

import java.util.UUID;

public class Orgunit extends SysEntity {

    public static final short NAME_ID      = (short) (1 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short BASEID_ID    = (short) (2 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short BASE_TYPE_ID = (short) (3 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short BASE_ID      = (short) (4 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short PARENTID_ID  = (short) (5 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short PARENT_ID    = (short) (6 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short CHILDS_ID    = (short) (7 << IdUtil.MEMBERID_SEQ_OFFSET);

    public Orgunit() {
        super(IdUtil.SYS_ORGUNIT_MODEL_ID);
    }

    private String _name;

    private UUID _baseId;

    private byte _baseType;

    public String getName() {
        return _name;
    }

    public void setName(String value) {
        if (!value.equals(_name)) {
            _name = value;
            onPropertyChanged(NAME_ID);
        }
    }

    public UUID getBaseId() {
        return _baseId;
    }

    public void setBaseId(UUID value) {
        if (!value.equals(_baseId)) {
            this._baseId = value;
            onPropertyChanged(BASEID_ID);
        }
    }

    public byte getBaseType() {
        return _baseType;
    }

    public void setBaseType(byte value) {
        if (value != _baseType) {
            this._baseType = value;
            onPropertyChanged(BASE_TYPE_ID);
        }
    }

    @Override
    public void writeMember(short id, IEntityMemberWriter bs, byte flags) throws Exception {
        switch (id) {
            case NAME_ID:
                bs.writeMember(id, _name, flags); break;
            case BASEID_ID:
                bs.writeMember(id, _baseId, flags); break;
            case BASE_TYPE_ID:
                bs.writeMember(id, _baseType, flags); break;
            default:
                throw new Exception("unknown member");
        }
    }

    @Override
    public void readMember(short id, IEntityMemberReader bs, int flags) throws Exception {
        switch (id) {
            case NAME_ID:
                _name = bs.readStringMember(flags); break;
            case BASEID_ID:
                _baseId = bs.readUUIDMember(flags); break;
            case BASE_TYPE_ID:
                _baseType = bs.readByteMember(flags); break;
            default:
                throw new Exception("unknown member");
        }
    }
}
