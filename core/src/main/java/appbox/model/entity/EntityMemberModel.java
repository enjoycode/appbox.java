package appbox.model.entity;

import appbox.data.PersistentState;
import appbox.model.EntityModel;
import appbox.serialization.*;

/**
 * 实体成员模型基类
 */
public abstract class EntityMemberModel implements IBinSerializable, IJsonSerializable {
    //region ====EntityMemberType====
    public enum EntityMemberType {
        DataField(0), EntityRef(2), EntitySet(3);
        public final byte value;

        EntityMemberType(int v) {
            value = (byte) v;
        }
    }
    //endregion

    public final EntityModel owner;     //不用序列化

    private   String          _name;
    private   String          _originalName;
    private   short           _memberId;
    protected boolean         _allowNull; //设计时改变时如果是DataField需要调用其onDataTypeChanged
    private   PersistentState _persistentState;
    private   String          _comment;

    /**
     * Only for serialization
     */
    public EntityMemberModel(EntityModel owner) {
        this.owner = owner;
    }

    public EntityMemberModel(EntityModel owner, String name, boolean allowNull) {
        this.owner       = owner;
        _name            = name;
        _allowNull       = allowNull;
        _persistentState = PersistentState.Detached;
    }

    //region ====Properties====
    public abstract EntityMemberType type();

    public final String name() {
        return _name;
    }

    public final String originalName() {
        return _originalName == null ? _name : _originalName;
    }

    public final short memberId() {
        return _memberId;
    }

    public final PersistentState persistentState() {
        return _persistentState;
    }

    public final boolean isNameChanged() {
        return _originalName != null && !_originalName.equals(_name);
    }

    public final boolean allowNull() {
        return _allowNull;
    }

    public abstract void setAllowNull(boolean value);
    //endregion

    //region ====Runtime Methods====
    //TODO:初始化运行时EntityMember实例
    //endregion

    //region ====Design Methods====
    public final void canAddTo(EntityModel owner) {
        if (this.owner != owner) {
            throw new RuntimeException("Owns by other");
        }
    }

    public final void initMemberId(short id) {
        if (_memberId == 0) {
            _memberId = id;
        } else {
            throw new RuntimeException("MemberId has init.");
        }
    }

    public final void renameTo(String newName) {
        //如果已经重命名过，不要再修改_originalName
        if (_originalName == null && _persistentState != PersistentState.Detached) {
            _originalName = _name;
        }
        _name = newName;
        onPropertyChanged();
    }

    public final void acceptChanges() {
        _persistentState = PersistentState.Unchnaged;
        _originalName    = null;
    }

    public final void markDeleted() {
        _persistentState = PersistentState.Deleted;
        owner.onPropertyChanged();
    }

    protected final void onPropertyChanged() {
        if (_persistentState == PersistentState.Unchnaged) {
            _persistentState = PersistentState.Modified;
            owner.onPropertyChanged();
        }
    }
    //endregion

    //region ====Serialization====
    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeBoolField(_allowNull, 2);
        bs.writeStringField(_name, 3);
        bs.writeShortField(_memberId, 4);
        if (_comment != null) {
            bs.writeStringField(_comment, 7);
        }

        if (owner.designMode()) {
            bs.writeStringField(_originalName, 5);
            bs.writeByteField(_persistentState.value, 6);
        }

        bs.finishWriteFields();
    }

    @Override
    public void readFrom(IInputStream bs) {
        int propIndex;
        do {
            propIndex = bs.readVariant();
            switch (propIndex) {
                case 2:
                    _allowNull = bs.readBool();
                    break;
                case 3:
                    _name = bs.readString();
                    break;
                case 4:
                    _memberId = bs.readShort();
                    break;
                case 5:
                    _originalName = bs.readString();
                    break;
                case 6:
                    _persistentState = PersistentState.fromValue(bs.readByte());
                    break;
                case 7:
                    _comment = bs.readString();
                    break;
                case 0:
                    break;
                default:
                    throw new RuntimeException("Unknown field id: " + propIndex);
            }

        } while (propIndex != 0);
    }

    @Override
    public final void writeToJson(IJsonWriter writer) {
        writer.startObject();

        writer.writeKeyValue("ID", _memberId);
        writer.writeKeyValue("AllowNull", _allowNull);
        writer.writeKeyValue("Comment", _comment);
        writer.writeKeyValue("Name", _name);
        writer.writeKeyValue("Type", type().value);
        //写入子类成员
        writeJsonMembers(writer);

        writer.endObject();
    }

    protected abstract void writeJsonMembers(IJsonWriter writer);
    //endregion
}
