package appbox.model;

import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.serialization.IBinSerializable;

import java.util.List;
import java.util.UUID;

/**
 * 模型的文件夹，每个应用的模型类型对应一个根文件夹
 */
public class ModelFolder implements IBinSerializable {

    private String            _name;
    private ModelFolder       _parent;
    private int               _appId;
    private ModelType         _targetModelType;
    private UUID              _id;
    private int               _sortNum;
    private int               _version;
    private List<ModelFolder> _childs;
    private boolean           _isDeleted;

    ModelFolder() { }

    ModelFolder(int appID, ModelType targetModelType) {
        this._appId           = appID;
        this._targetModelType = targetModelType;
    }

    ModelFolder(ModelFolder parent, String name) {
        this._id         = UUID.randomUUID();
        _appId           = parent._appId;
        _parent          = parent;
        _name            = name;
        _targetModelType = parent._targetModelType;
        _parent._childs.add(this);
    }

    //region ====Properties====
    public boolean hasChild() {
        return _childs != null && _childs.size() > 0;
    }

    public String getName() {
        return _name;
    }

    public void setName(String _name) {
        this._name = _name;
    }

    public ModelFolder getParent() {
        return _parent;
    }

    public void setParent(ModelFolder _parent) {
        this._parent = _parent;
    }

    public int getAppId() {
        return _appId;
    }

    public void setAppId(int _appId) {
        this._appId = _appId;
    }

    public ModelType getTargetModelType() {
        return _targetModelType;
    }

    public void setTargetModelType(ModelType _targetModelType) {
        this._targetModelType = _targetModelType;
    }

    public UUID getId() {
        return _id;
    }

    public void setId(UUID _id) {
        this._id = _id;
    }

    public int getSortNum() {
        return _sortNum;
    }

    public void setSortNum(int _sortNum) {
        this._sortNum = _sortNum;
    }

    public int getVersion() {
        return _version;
    }

    public void setVersion(int _version) {
        this._version = _version;
    }

    public List<ModelFolder> getChilds() {
        return _childs;
    }

    public void setChilds(List<ModelFolder> _childs) {
        this._childs = _childs;
    }

    public boolean isDeleted() {
        return _isDeleted;
    }

    public void setDeleted(boolean deleted) {
        _isDeleted = deleted;
    }
    //endregion

    //region ====Serialization====
    @Override
    public void writeTo(BinSerializer bs) {
        if (_parent != null) {
            bs.writeUUID(_id, 1);
            bs.writeString(_name, 2);
            bs.serialize(_parent, 4);
            if (_targetModelType == ModelType.Permission) //仅权限文件夹排序
                bs.writeInt(_sortNum, 8);
        } else {
            bs.writeInt(_version, 3);
            bs.writeBool(_isDeleted, 9);
        }
        if (hasChild())
            bs.writeList(_childs, 5);
        bs.writeInt(_appId, 6);
        bs.writeByte(_targetModelType.value, 7);

        bs.writeInt(0);
    }

    @Override
    public void readFrom(BinDeserializer bs) {
        int propIndex;
        do {
            propIndex = bs.readVariant();
            switch (propIndex) {
                case 1:
                    _id = bs.readUUID();
                    break;
                case 2:
                    _name = bs.readString();
                    break;
                case 3:
                    _version = bs.readInt();
                    break;
                case 4:
                    _parent = (ModelFolder) bs.deserialize();
                    break;
                case 5:
                    _childs = bs.readList(ModelFolder::new);
                    break;
                case 0:
                    break;
                default:
                    throw new RuntimeException("Unknown field id:" + propIndex);
            }
        } while (propIndex != 0);
    }
    //endregion
}
