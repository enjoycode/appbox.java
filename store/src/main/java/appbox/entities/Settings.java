package appbox.entities;

import appbox.data.EntityId;
import appbox.data.SysEntity;
import appbox.data.SysUniqueIndex;
import appbox.exceptions.UnknownEntityMember;
import appbox.expressions.KVFieldExpression;
import appbox.model.entity.DataFieldModel;
import appbox.serialization.IEntityMemberReader;
import appbox.serialization.IEntityMemberWriter;
import appbox.utils.IdUtil;

public final class Settings extends SysEntity {
    public static final short APPID_ID  = (short) (1 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short USERID_ID = (short) (2 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short CATLOG_ID = (short) (3 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short NAME_ID   = (short) (4 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short TYPE_ID   = (short) (5 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short VALUE_ID  = (short) (6 << IdUtil.MEMBERID_SEQ_OFFSET);

    public static final KVFieldExpression APPID  = new KVFieldExpression(APPID_ID, DataFieldModel.DataFieldType.Int);
    public static final KVFieldExpression USERID = new KVFieldExpression(USERID_ID, DataFieldModel.DataFieldType.EntityId);
    public static final KVFieldExpression NAME   = new KVFieldExpression(NAME_ID, DataFieldModel.DataFieldType.String);

    private int      _appId;
    private EntityId _userId;
    private String   _catlog;
    private String   _name;
    private String   _type;
    private byte[]   _value;

    public int getAppId() {return _appId; }

    public void setAppId(int appId) { _appId = appId; }

    public EntityId getUserId() { return _userId; }

    public void setUserId(EntityId userId) { _userId = userId; }

    public String getCatlog() { return _catlog; }

    public void setCatlog(String catlog) { _catlog = catlog; }

    public String getName() { return _name; }

    public void setName(String name) { _name = name; }

    public String getType() { return _type; }

    public void setType(String type) { _type = type; }

    public byte[] getValue() {return _value;}

    public void setValue(byte[] value) { _value = value; }

    public Settings() {
        super(IdUtil.SYS_SETTINGS_MODEL_ID);
    }

    @Override
    public void writeMember(short id, IEntityMemberWriter bs, byte flags) {
        switch (id) {
            case APPID_ID:
                bs.writeMember(id, _appId, flags); break;
            case USERID_ID:
                bs.writeMember(id, _userId, flags); break;
            case CATLOG_ID:
                bs.writeMember(id, _catlog, flags); break;
            case NAME_ID:
                bs.writeMember(id, _name, flags); break;
            case TYPE_ID:
                bs.writeMember(id, _type, flags); break;
            case VALUE_ID:
                bs.writeMember(id, _value, flags); break;
            default:
                throw new UnknownEntityMember(Settings.class, id);
        }
    }

    @Override
    public void readMember(short id, IEntityMemberReader bs, int flags) {
        switch (id) {
            case APPID_ID:
                _appId = bs.readIntMember(flags); break;
            case USERID_ID:
                _userId = bs.readEntityIdMember(flags); break;
            case CATLOG_ID:
                _catlog = bs.readStringMember(flags); break;
            case NAME_ID:
                _name = bs.readStringMember(flags); break;
            case TYPE_ID:
                _type = bs.readStringMember(flags); break;
            case VALUE_ID:
                _value = bs.readBinaryMember(flags); break;
            default:
                throw new UnknownEntityMember(Settings.class, id);
        }
    }

    //====二级索引====
    public static final class UI_Settings extends SysUniqueIndex<Settings> {
        public static final byte INDEXID = (byte) ((1 << IdUtil.INDEXID_UNIQUE_OFFSET) | (1 << 2));

        private int      _appId;
        private EntityId _userId = new EntityId();
        private String   _name;

        @Override
        public void readMember(short id, IEntityMemberReader bs, int flags) {
            switch (id) {
                case APPID_ID:
                    _appId = bs.readIntMember(flags); break;
                case USERID_ID:
                    _userId = bs.readEntityIdMember(flags); break;
                case NAME_ID:
                    _name = bs.readStringMember(flags); break;
                default:
                    throw new UnknownEntityMember(Employee.class, id);
            }
        }
    }
}
