package appbox.model;

import appbox.serialization.IBinSerializable;
import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 模型的文件夹，每个应用的模型类型对应一个根文件夹
 */
public class ModelFolder implements IBinSerializable {

    private UUID              _id;               //root = null
    private String            _name;             //root = null
    private ModelFolder       _parent;           //root = null
    private int               _appId;            //仅root序列化
    private ModelType         _targetModelType;  //仅root序列化
    private int               _sortNum;          //仅child有效
    private List<ModelFolder> _childs;
    private int               _version;          //仅root有效
    private boolean           _isDeleted;        //仅root有效

    public ModelFolder() { }

    private ModelFolder(ModelFolder parent) {
        _parent          = parent;
        _appId           = parent._appId;
        _targetModelType = parent._targetModelType;
    }

    /** Create root folder */
    public ModelFolder(int appID, ModelType targetModelType) {
        this._appId           = appID;
        this._targetModelType = targetModelType;
    }

    /** Create child folder */
    public ModelFolder(ModelFolder parent, String name) {
        _id              = UUID.randomUUID();
        _appId           = parent._appId;
        _parent          = parent;
        _name            = name;
        _targetModelType = parent._targetModelType;
        _parent._childs.add(this);
    }

    //region ====Properties====
    public boolean hasChild() { return _childs != null && _childs.size() > 0;}

    public String getName() { return _name;}

    public void setName(String _name) { this._name = _name;}

    public ModelFolder getParent() { return _parent;}

    public int getAppId() { return _appId;}

    public ModelType getTargetModelType() { return _targetModelType;}

    public UUID getId() { return _id;}

    public int getSortNum() { return _sortNum;}

    public void setSortNum(int _sortNum) { _sortNum = _sortNum;}

    public int getVersion() { return _version;}

    public void setVersion(int _version) { _version = _version;}

    public List<ModelFolder> getChilds() {
        if (_childs == null)
            _childs = new ArrayList<>();
        return _childs;
    }

    public boolean isDeleted() { return _isDeleted;}

    public void setDeleted(boolean deleted) { _isDeleted = deleted;}

    public ModelFolder getRoot() { return _parent == null ? this : _parent.getRoot();}
    //endregion

    //region ====Serialization====
    @Override
    public void writeTo(IOutputStream bs) {
        if (_parent != null) {
            bs.writeUUIDField(_id, 1);
            bs.writeStringField(_name, 2);
            if (_targetModelType == ModelType.Permission) //仅权限文件夹排序
                bs.writeIntField(_sortNum, 8);
        } else {
            bs.writeIntField(_version, 3);
            bs.writeIntField(_appId, 6);
            bs.writeByteField(_targetModelType.value, 7);
            bs.writeBoolField(_isDeleted, 9);
        }
        if (hasChild())
            bs.writeList(_childs, 5, false);

        bs.finishWriteFields();
    }

    @Override
    public void readFrom(IInputStream bs) {
        int propIndex;
        do {
            propIndex = bs.readVariant();
            switch (propIndex) {
                case 1:
                    _id = bs.readUUID(); break;
                case 2:
                    _name = bs.readString(); break;
                case 3:
                    _version = bs.readInt(); break;
                case 5:
                    _childs = bs.readList(() -> new ModelFolder(this), false); break;
                case 6:
                    _appId = bs.readInt(); break;
                case 7:
                    _targetModelType = ModelType.fromValue(bs.readByte()); break;
                case 8:
                    _sortNum = bs.readInt(); break;
                case 9:
                    _isDeleted = bs.readBool(); break;
                case 0:
                    break;
                default:
                    throw new RuntimeException("Unknown field id:" + propIndex);
            }
        } while (propIndex != 0);
    }
    //endregion
}
