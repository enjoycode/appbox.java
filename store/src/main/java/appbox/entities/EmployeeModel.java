package appbox.entities;

import appbox.data.SysEntity;
import appbox.serialization.IEntityMemberReader;
import appbox.serialization.IEntityMemberWriter;
import appbox.utils.IdUtil;

import java.util.Date;

public class EmployeeModel extends SysEntity {

    public static final short NAME_ID     = (short) (1 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short MALE_ID     = (short) (2 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short BIRTHDAY_ID = (short) (3 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short ACCOUNT_ID  = (short) (4 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short PASSWORD_ID = (short) (5 << IdUtil.MEMBERID_SEQ_OFFSET);

    private String _name;

    private boolean _male;

    private Date _birthday;

    private String _account;

    private byte[] _password;

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        if (!name.equals(_name)) {
            this._name = name;
            onPropertyChanged(NAME_ID);
        }
    }

    public boolean isMale() {
        return _male;
    }

    public void setMale(boolean male) {
        if (male!=_male) {
            this._male = male;
            onPropertyChanged(MALE_ID);
        }

    }

    public Date getBirthday() {
        return _birthday;
    }

    public void setBirthday(Date birthday) {
        if (!birthday.equals(_birthday)) {
            this._birthday = birthday;
            onPropertyChanged(BIRTHDAY_ID);
        }
    }

    public String getAccount() {
        return _account;
    }

    public void setAccount(String account) {
        if (!account.equals(_account)) {
            this._account = account;
            onPropertyChanged(ACCOUNT_ID);
        }
    }

    public byte[] getPassword() {
        return _password;
    }

    public void setPassword(byte[] password) {
        if (!password.equals(_password)) {
            this._password = password;
            onPropertyChanged(PASSWORD_ID);
        }
    }

    public EmployeeModel(long modelId) {
        super(IdUtil.SYS_EMPLOYEE_MODEL_ID);
    }

    @Override
    public void writeMember(short id, IEntityMemberWriter bs, byte flags) throws Exception {
        switch (id) {
            case NAME_ID:
                bs.writeMember(id, _name, flags); break;
            case MALE_ID:
                bs.writeMember(id, _male, flags); break;
            case BIRTHDAY_ID:
                bs.writeMember(id, _birthday, flags); break;
            case ACCOUNT_ID:
                bs.writeMember(id, _account, flags); break;
            case PASSWORD_ID:
                bs.writeMember(id, _password, flags); break;
            default:
                throw new Exception("unknown member");
        }
    }

    @Override
    public void readMember(short id, IEntityMemberReader bs, int flags) throws Exception {
        switch (id) {
            case NAME_ID:
                _name = bs.readStringMember(flags); break;
            case MALE_ID:
                _male = bs.readBoolMember(flags); break;
            case BIRTHDAY_ID:
                _birthday = bs.readDateMember(flags); break;
            case ACCOUNT_ID:
                _account = bs.readStringMember(flags); break;
            case PASSWORD_ID:
                _password = bs.readBinaryMember(flags); break;
            default:
                throw new Exception("unknown member");
        }
    }
}
