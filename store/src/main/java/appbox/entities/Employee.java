package appbox.entities;

import appbox.data.EntityId;
import appbox.data.SysEntity;
import appbox.data.SysUniqueIndex;
import appbox.exceptions.UnknownEntityMember;
import appbox.expressions.KVFieldExpression;
import appbox.model.entity.DataFieldModel;
import appbox.serialization.IEntityMemberReader;
import appbox.serialization.IEntityMemberWriter;
import appbox.store.EntityStore;
import appbox.utils.IdUtil;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class Employee extends SysEntity {

    public static final long MODELID = IdUtil.SYS_EMPLOYEE_MODEL_ID;

    public static final short NAME_ID     = (short) (1 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short MALE_ID     = (short) (2 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short BIRTHDAY_ID = (short) (3 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short ACCOUNT_ID  = (short) (4 << IdUtil.MEMBERID_SEQ_OFFSET);
    public static final short PASSWORD_ID = (short) (5 << IdUtil.MEMBERID_SEQ_OFFSET);

    public static final KVFieldExpression NAME     = new KVFieldExpression(NAME_ID, DataFieldModel.DataFieldType.String);
    public static final KVFieldExpression ACCOUNT  = new KVFieldExpression(ACCOUNT_ID, DataFieldModel.DataFieldType.String);
    public static final KVFieldExpression PASSWORD = new KVFieldExpression(PASSWORD_ID, DataFieldModel.DataFieldType.Binary);

    private String        _name;
    private boolean       _male;
    private LocalDateTime _birthday;
    private String        _account;
    private byte[]        _password;

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        if (!name.equals(_name)) {
            this._name = name;
            onPropertyChanged(NAME_ID);
        }
    }

    public boolean getMale() {
        return _male;
    }

    public void setMale(boolean male) {
        if (male != _male) {
            this._male = male;
            onPropertyChanged(MALE_ID);
        }

    }

    public LocalDateTime getBirthday() {
        return _birthday;
    }

    public void setBirthday(LocalDateTime birthday) {
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
        if (!Arrays.equals(password, _password)) {
            this._password = password;
            onPropertyChanged(PASSWORD_ID);
        }
    }

    @Override
    public long modelId() {
        return MODELID;
    }

    @Override
    public void writeMember(short id, IEntityMemberWriter bs, byte flags) {
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
                throw new UnknownEntityMember(Employee.class, id);
        }
    }

    @Override
    public void readMember(short id, IEntityMemberReader bs, int flags) {
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
                throw new UnknownEntityMember(Employee.class, id);
        }
    }

    public static CompletableFuture<Employee> fetchAsync(EntityId id) {
        return EntityStore.loadAsync(Employee.class, id);
    }

    //====二级索引====
    public static final class UI_Account extends SysUniqueIndex<Employee> {
        public static final byte INDEXID = (byte) ((1 << IdUtil.INDEXID_UNIQUE_OFFSET) | (1 << 2));

        private String _account;
        private byte[] _password;

        public String getAccount() {
            return _account;
        }

        public byte[] getPassword() {
            return _password;
        }

        @Override
        public void readMember(short id, IEntityMemberReader bs, int flags) {
            switch (id) {
                case ACCOUNT_ID:
                    _account = bs.readStringMember(flags); break;
                case PASSWORD_ID:
                    _password = bs.readBinaryMember(flags); break;
                default:
                    throw new UnknownEntityMember(Employee.class, id);
            }
        }
    }
}
