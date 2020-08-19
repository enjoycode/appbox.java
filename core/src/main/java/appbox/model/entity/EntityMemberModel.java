package appbox.model.entity;

import appbox.data.PersistentState;
import appbox.model.EntityModel;
import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.serialization.IBinSerializable;

public abstract class EntityMemberModel implements IBinSerializable {
    public enum EntityMemberType {
        DataField, EntityRef, EntitySet;
    }

    protected EntityModel     owner;     //不用序列化
    private   String          _name;
    private   String          _originalName;
    private   short           _memberId;
    private   boolean         _allowNull; //设计时改变时如果是DataField需要调用其onDataTypeChanged
    private   PersistentState _persistentState;
    private   String          _comment;

    /**
     * Only for serialization
     */
    public EntityMemberModel() {
    }

    public EntityMemberModel(EntityModel owner, String name) {
        this.owner = owner;
        _name      = name;
    }

    //region ====Properties====
    public abstract EntityMemberType type();

    public String name() {
        return _name;
    }

    public String originalName() {
        return _originalName == null ? _name : _originalName;
    }

    public short memberId() {
        return _memberId;
    }

    public PersistentState persistentState() {
        return _persistentState;
    }

    public boolean isNameChanged() {
        return _originalName != null && !_originalName.equals(_name);
    }
    //endregion

    //region ====Runtime Methods====
    //TODO:初始化运行时EntityMember实例
    //endregion

    //region ====Design Methods====
    public void initMemberId(short id) {
        if (_memberId == 0) {
            _memberId = id;
        }
    }

    public void acceptChanges() {
        _persistentState = PersistentState.Unchnaged;
        _originalName    = null;
    }

    public void markDeleted() {
        _persistentState = PersistentState.Deleted;
        owner.onPropertyChanged();
    }

    protected void onPropertyChanged() {
        if (_persistentState == PersistentState.Unchnaged) {
            _persistentState = PersistentState.Modified;
            owner.onPropertyChanged();
        }
    }
    //endregion

    //region ====Serialization====
    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        //TODO:
    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {
        //TODO:
    }
    //endregion
}
