package appbox.entities;

import appbox.data.EntityId;
import appbox.data.PersistentState;
import appbox.data.SysEntity;
import appbox.exceptions.UnknownEntityMember;
import appbox.expressions.KVFieldExpression;
import appbox.model.entity.DataFieldModel;
import appbox.serialization.IEntityMemberReader;
import appbox.serialization.IEntityMemberWriter;
import appbox.utils.IdUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OrgUnit extends SysEntity {

    public static final long MODELID = IdUtil.SYS_ORGUNIT_MODEL_ID;

    public static final short NAME_ID      = (short) (1 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short BASEID_ID    = (short) (2 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short BASE_TYPE_ID = (short) (3 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short BASE_ID      = (short) (4 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short PARENTID_ID  = (short) (5 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short PARENT_ID    = (short) (6 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short CHILDS_ID    = (short) (7 << IdUtil.MEMBERID_SEQ_OFFSET);

    public static final KVFieldExpression NAME   = new KVFieldExpression(NAME_ID, DataFieldModel.DataFieldType.String);
    public static final KVFieldExpression BASEID = new KVFieldExpression(BASEID_ID, DataFieldModel.DataFieldType.EntityId);

    private String        _name;
    private EntityId      _baseId;
    private long          _baseType;
    private EntityId      _parentId;
    private OrgUnit       _parent;
    private List<OrgUnit> _childs;

    public String getName() {
        return _name;
    }

    public void setName(String value) {
        if (!value.equals(_name)) {
            _name = value;
            onPropertyChanged(NAME_ID);
        }
    }

    public EntityId getBaseId() {
        return _baseId;
    }

    public void setBaseId(EntityId value) {
        if (!value.equals(_baseId)) {
            this._baseId = value;
            onPropertyChanged(BASEID_ID);
        }
    }

    public long getBaseType() {
        return _baseType;
    }

    public void setBaseType(long value) {
        if (value != _baseType) {
            this._baseType = value;
            onPropertyChanged(BASE_TYPE_ID);
        }
    }

    public EntityId getParentId() {return _parentId;}

    public void setParentId(EntityId value) {
        if (!Objects.equals(_parentId, value)) {
            _parentId = value;
            _parent   = null;
            onPropertyChanged(PARENTID_ID);
        }
    }

    public OrgUnit getParent() {
        if (_parentId != null && _parent == null)
            throw new RuntimeException("EntityRef hasn't loaded.");
        return _parent;
    }

    public void setParent(OrgUnit parent) {
        if (parent == null) {
            setParentId(null);
        } else {
            setParentId(parent.id());
            _parent = parent;
        }
    }

    public List<OrgUnit> getChilds() {
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
    public long modelId() {
        return MODELID;
    }

    @Override
    public Object getNaviPropForFetch(String propName) {
        switch (propName) {
            case "Parent":
                if (_parent == null)
                    _parent = new OrgUnit();
                return _parent;
            case "Childs":
                if (_childs == null)
                    _childs = new ArrayList<>();
                return _childs;
            default:
                throw new RuntimeException("Unknown member: " + propName);
        }
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
            case PARENT_ID:
                bs.writeMember(id, _parent, flags); break;
            case CHILDS_ID:
                bs.writeMember(id, _childs, flags); break;
            default:
                throw new UnknownEntityMember(OrgUnit.class, id);
        }
    }

    @Override
    public void readMember(short id, IEntityMemberReader bs, int flags) {
        switch (id) {
            case NAME_ID:
                _name = bs.readStringMember(flags); break;
            case BASEID_ID:
                _baseId = bs.readEntityIdMember(flags); break;
            case BASE_TYPE_ID:
                _baseType = bs.readLongMember(flags); break;
            case PARENTID_ID:
                _parentId = bs.readEntityIdMember(flags); break;
            case PARENT_ID:
                _parent = bs.readRefMember(flags, OrgUnit::new); break;
            case CHILDS_ID:
                _childs = bs.readSetMember(flags, OrgUnit::new); break;
            default:
                throw new UnknownEntityMember(OrgUnit.class, id);
        }
    }
}
