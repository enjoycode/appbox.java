import appbox.data.SqlEntity;
import appbox.exceptions.UnknownEntityMember;
import appbox.serialization.IEntityMemberReader;
import appbox.serialization.IEntityMemberWriter;
import appbox.utils.IdUtil;

/**
 * 测试用映射至Sql存储的实体
 */
public class ELog extends SqlEntity {
    public static final long MODELID = 8888L;

    public static final short ID_ID   = (short) (1 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short NAME_ID = (short) (2 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short ADDR_ID = (short) (3 << IdUtil.MEMBERID_SEQ_OFFSET);

    public static final short MSG_ID = (short) (4 << IdUtil.MEMBERID_SEQ_OFFSET);

    private int    _id;
    private String _name;
    private String _address;

    private String _msg;

    public int getId() {return _id;}

    public void setId(int value) {_id = value;}

    public String getName() {return _name;}

    public void setName(String value) {_name = value;}

    public String getAddress() {return _address;}

    public void setAddress(String value) {_address = value;}

    public String getMsg() {
        return _msg;
    }

    public void setMsg(String msg) {
        this._msg = _msg;
    }

    @Override
    public long modelId() {
        return MODELID;
    }

    @Override
    public void writeMember(short id, IEntityMemberWriter bs, byte flags) {
        switch (id) {
            case ID_ID:
                bs.writeMember(id, _id, flags); break;
            case NAME_ID:
                bs.writeMember(id, _name, flags); break;
            case ADDR_ID:
                bs.writeMember(id, _address, flags); break;
            default:
                throw new UnknownEntityMember(ELog.class, id);
        }
    }

    @Override
    public void readMember(short id, IEntityMemberReader bs, int flags) {
        switch (id) {
            case ID_ID:
                _id = bs.readIntMember(flags); break;
            case NAME_ID:
                _name = bs.readStringMember(flags); break;
            case ADDR_ID:
                _address = bs.readStringMember(flags); break;
            default:
                throw new UnknownEntityMember(ELog.class, id);
        }
    }
}
