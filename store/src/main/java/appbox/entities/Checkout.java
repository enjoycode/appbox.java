package appbox.entities;

import appbox.data.SysEntity;
import appbox.exceptions.UnknownEntityMember;
import appbox.expressions.KVFieldExpression;
import appbox.model.entity.DataFieldModel;
import appbox.serialization.IEntityMemberReader;
import appbox.serialization.IEntityMemberWriter;
import appbox.utils.IdUtil;

import java.util.UUID;

public final class Checkout extends SysEntity {
    public static final long MODELID = IdUtil.SYS_CHECKOUT_MODEL_ID;

    public static final short NODE_TYPE_ID      = (short) (1 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short TARGET_ID         = (short) (2 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short DEVELOPER_ID      = (short) (3 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short DEVELOPER_NAME_ID = (short) (4 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short VERSION_ID        = (short) (5 << IdUtil.MEMBERID_SEQ_OFFSET);

    public static final KVFieldExpression DEVELOPER = new KVFieldExpression(DEVELOPER_ID, DataFieldModel.DataFieldType.Guid);

    private byte   _nodeType;
    private String _targetId;
    private UUID   _developerId;
    private String _developerName;
    private int    _version;

    public byte getNodeType() {
        return _nodeType;
    }

    public void setNodeType(byte value) {
        if (value != _nodeType) {
            this._nodeType = value;
            onPropertyChanged(NODE_TYPE_ID);
        }
    }

    public String getTargetId() {
        return _targetId;
    }

    public void setTargetId(String value) {
        if (!value.equals(_targetId)) {
            this._targetId = value;
            onPropertyChanged(TARGET_ID);
        }
    }

    public UUID getDeveloperId() {
        return _developerId;
    }

    public void setDeveloperId(UUID value) {
        if (!value.equals(_developerId)) {
            this._developerId = value;
            onPropertyChanged(DEVELOPER_ID);
        }
    }

    public String getDeveloperName() {
        return _developerName;
    }

    public void setDeveloperName(String value) {
        if (!value.equals(_developerName)) {
            this._developerName = value;
            onPropertyChanged(DEVELOPER_NAME_ID);
        }
    }

    public int getVersion() {
        return _version;
    }

    public void setVersion(int value) {
        if (value != _version) {
            this._version = value;
            onPropertyChanged(VERSION_ID);
        }
    }

    @Override
    public long modelId() {
        return MODELID;
    }

    @Override
    public void writeMember(short id, IEntityMemberWriter bs, byte flags) {
        switch (id) {
            case NODE_TYPE_ID:
                bs.writeMember(id, _nodeType, flags); break;
            case TARGET_ID:
                bs.writeMember(id, _targetId, flags); break;
            case DEVELOPER_ID:
                bs.writeMember(id, _developerId, flags); break;
            case DEVELOPER_NAME_ID:
                bs.writeMember(id, _developerName, flags); break;
            case VERSION_ID:
                bs.writeMember(id, _version, flags); break;
            default:
                throw new UnknownEntityMember(Checkout.class, id);
        }
    }

    @Override
    public void readMember(short id, IEntityMemberReader bs, int flags) {
        switch (id) {
            case NODE_TYPE_ID:
                _nodeType = bs.readByteMember(flags); break;
            case TARGET_ID:
                _targetId = bs.readStringMember(flags); break;
            case DEVELOPER_ID:
                _developerId = bs.readUUIDMember(flags); break;
            case DEVELOPER_NAME_ID:
                _developerName = bs.readStringMember(flags); break;
            case VERSION_ID:
                _version = bs.readIntMember(flags); break;
            default:
                throw new UnknownEntityMember(Checkout.class, id);
        }
    }
}
