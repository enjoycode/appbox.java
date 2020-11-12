package appbox.entities;

import appbox.data.SysEntity;
import appbox.serialization.IEntityMemberReader;
import appbox.serialization.IEntityMemberWriter;
import appbox.utils.IdUtil;

public class Staged extends SysEntity {

    public static final short TYPE_ID      = (short) (1 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short MODEL_ID     = (short) (2 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short DEVELOPER_ID = (short) (3 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short DATA_ID      = (short) (4 << IdUtil.MEMBERID_SEQ_OFFSET);

    private Byte _type;

    private String _modelId;

    private String _developerId;

    private String _data;

    public Staged() {
        super(IdUtil.SYS_STAGED_MODEL_ID);
    }

    public Byte getType() {
        return _type;
    }

    public void setType(Byte value) {
        if(!value.equals(_type)) {
            this._type = value;
            onPropertyChanged(TYPE_ID);
        }
    }

    public String getModelId() {
        return _modelId;
    }

    public void setModelId(String value) {
        if(!value.equals(_modelId)) {
            this._modelId = value;
            onPropertyChanged(MODEL_ID);
        }
    }

    public String getDeveloperId() {
        return _developerId;
    }

    public void setDeveloperId(String value) {
        if(!value.equals(_developerId)) {
            this._developerId = value;
            onPropertyChanged(DEVELOPER_ID);
        }
    }

    public String getData() {
        return _data;
    }

    public void setData(String value) {
        if(!value.equals(_data)) {
            this._data = value;
            onPropertyChanged(DATA_ID);
        }
    }

    @Override
    public void writeMember(short id, IEntityMemberWriter bs, byte flags) throws Exception {
        switch (id) {
            case TYPE_ID:
                bs.writeMember(id, _type, flags); break;
            case MODEL_ID:
                bs.writeMember(id, _modelId, flags); break;
            case DEVELOPER_ID:
                bs.writeMember(id, _developerId, flags); break;
            case DATA_ID:
                bs.writeMember(id, _data, flags); break;
            default:
                throw new Exception("unknown member");
        }
    }

    @Override
    public void readMember(short id, IEntityMemberReader bs, int flags) throws Exception {
        switch (id) {
            case TYPE_ID:
                _type=bs.readByteMember(flags); break;
            case MODEL_ID:
                _modelId=bs.readStringMember(flags); break;
            case DEVELOPER_ID:
                _developerId=bs.readStringMember(flags); break;
            case DATA_ID:
                _data=bs.readStringMember(flags); break;
            default:
                throw new Exception("unknown member");
        }
    }
}
