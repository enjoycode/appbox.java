package appbox.entities;

import appbox.data.EntityId;
import appbox.data.PersistentState;
import appbox.data.SysEntity;
import appbox.serialization.IEntityMemberReader;
import appbox.serialization.IEntityMemberWriter;
import appbox.utils.IdUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    private String        _name;
    private UUID          _baseId;
    private byte          _baseType;
    private EntityId      _parentId;
    private Orgunit       _parent;
    private List<Orgunit> _childs;

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

    public EntityId getParentId() { return _parentId; }

    public void setParentId(EntityId value) {
        if (!Objects.equals(_parentId, value)) {
            _parentId = value;
            _parent   = null;
            onPropertyChanged(PARENTID_ID);
        }
    }

    public Orgunit getParent() {
        if (_parentId != null && _parent == null)
            throw new RuntimeException("EntityRef hasn't loaded.");
        return _parent;
    }

    public void setParent(Orgunit parent) {
        if (parent == null) {
            setParentId(null);
        } else {
            setParentId(parent.id());
            _parent = parent;
        }
    }

    public List<Orgunit> getChilds() {
        if (_childs == null) {
            if (persistentState() == PersistentState.Detached) {
                _childs = new ArrayList<>();
            } else {
                throw new RuntimeException("EntitySet hasn't loaded.");
            }
        }

        return _childs;
    }

    @Override
    public void writeMember(short id, IEntityMemberWriter bs, byte flags) {
        switch (id) {
            case NAME_ID:
                bs.writeMember(id, _name, flags); break;
            case BASEID_ID:
                bs.writeMember(id, _baseId, flags); break;
            case BASE_TYPE_ID:
                bs.writeMember(id, _baseType, flags); break;
            case PARENTID_ID:
                bs.writeMember(id, _parentId, flags); break;
            default:
                throw new RuntimeException("unknown member");
        }
    }

    @Override
    public void readMember(short id, IEntityMemberReader bs, int flags) {
        switch (id) {
            case NAME_ID:
                _name = bs.readStringMember(flags); break;
            case BASEID_ID:
                _baseId = bs.readUUIDMember(flags); break;
            case BASE_TYPE_ID:
                _baseType = bs.readByteMember(flags); break;
            case PARENTID_ID:
                _parentId = bs.readEntityIdMember(flags); break;
            default:
                throw new RuntimeException("unknown member");
        }
    }
}
