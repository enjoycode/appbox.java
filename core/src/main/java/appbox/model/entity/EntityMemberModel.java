package appbox.model.entity;

import appbox.data.PersistentState;
import appbox.model.EntityModel;
import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.serialization.IBinSerializable;

/**
 * 实体成员模型基类
 */
public abstract class EntityMemberModel implements IBinSerializable {
    public enum EntityMemberType {
        DataField(0), EntityRef(2), EntitySet(3);
        public final byte value;

        EntityMemberType(int v) {
            value = (byte) v;
        }
    }

    protected final EntityModel owner;     //不用序列化

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

    public PersistentState persistentState() {
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
    public final void canAddTo(EntityModel owner) throws RuntimeException {
        if (this.owner != owner) {
            throw new RuntimeException();
        }
    }

    public final void initMemberId(short id) throws Exception {
        if (_memberId == 0) {
            _memberId = id;
        } else {
            throw new Exception("MemberId has init.");
        }
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
    public void writeTo(BinSerializer bs) throws Exception {
        bs.writeBool(_allowNull, 2);
        bs.writeString(_name, 3);
        bs.writeShort(_memberId, 4);
        if (_comment != null) {
            bs.writeString(_comment, 7);
        }

        if (owner.designMode()) {
            bs.writeString(_originalName, 5);
            bs.writeByte(_persistentState.value, 6);
        }

        bs.finishWriteFields();
    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {
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
    //endregion
}
