package appbox.model;

import appbox.data.PersistentState;
import appbox.serialization.IBinSerializable;
import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;
import appbox.utils.IdUtil;

import java.util.UUID;

/**
 * 模型基类，实例分为设计时与运行时
 */
public abstract class ModelBase implements IBinSerializable {
    protected long            _id;
    private   String          _name;
    private   String          _originalName;
    private   boolean         _designMode;
    private   int             _version;
    private   PersistentState _persistentState;
    private   UUID            _folderId;

    /**
     * only for Serialization
     */
    public ModelBase() {}

    public ModelBase(long id, String name) {
        _designMode      = true;
        _id              = id;
        _name            = name;
        _persistentState = PersistentState.Detached;
    }

    //region ====Properties====
    public final ModelLayer modelLayer() {
        return ModelLayer.fromValue((byte) (_id & IdUtil.MODELID_LAYER_MASK));
    }

    public final long id() {
        return _id;
    }

    public final int appId() {
        return IdUtil.getAppIdFromModelId(_id);
    }

    public final String name() {
        return _name;
    }

    public abstract ModelType modelType();

    public final boolean designMode() {
        return _designMode;
    }

    public final int version() {
        return _version;
    }

    public final PersistentState persistentState() {
        return _persistentState;
    }

    public final UUID getFolderId() {
        return _folderId;
    }

    public final void setFolderId(UUID folderId) {
        _folderId = folderId;
    }

    public final String originalName() {
        return _originalName == null ? _name : _originalName;
    }
    //endregion

    //region ====Design Methods====
    public final boolean isNameChanged() {
        return _originalName != null && !_originalName.equals(_name);
    }

    public final void increaseVersion() {
        _version += 1;
    }

    public final void checkDesignMode() throws RuntimeException {
        if (!designMode()) {
            throw new RuntimeException();
        }
    }

    public final void onPropertyChanged() {
        if (_persistentState == PersistentState.Unchnaged) {
            _persistentState = PersistentState.Modified;
        }
    }

    public void acceptChanges() {
        if (_persistentState != PersistentState.Unchnaged) {
            if (_persistentState == PersistentState.Deleted) {
                _persistentState = PersistentState.Detached;
            } else {
                _persistentState = PersistentState.Unchnaged;
            }

            _originalName = null;
        }
    }

    public final void markDeleted() {
        if (_persistentState != PersistentState.Detached)
            _persistentState = PersistentState.Deleted;
    }
    //endregion

    //region ====Serialization====
    public static ModelBase makeModelByType(byte type) {
        var modelType = ModelType.fromValue(type);
        switch (modelType) {
            case Entity:
                return new EntityModel();
            case Service:
                return new ServiceModel();
            case View:
                return new ViewModel();
            case Permission:
                return new PermissionModel();
            case DataStore:
                return new DataStoreModel();
            default:
                throw new RuntimeException("Unknown model type: " + type);
        }
    }

    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeLongField(_id, 1);
        bs.writeStringField(_name, 2);
        bs.writeBoolField(_designMode, 3);

        if (_designMode) {
            bs.writeIntField(_version, 4);
            bs.writeByteField(_persistentState.value, 5);
            //TODO: folder
            if (_originalName != null) {
                bs.writeStringField(_originalName, 7);
            }
        } else if (modelType() == ModelType.Permission) {
            //TODO: folder
        }

        bs.finishWriteFields();
    }

    @Override
    public void readFrom(IInputStream bs) {
        int propIndex;
        do {
            propIndex = bs.readVariant();
            switch (propIndex) {
                case 1:
                    _id = bs.readLong();
                    break;
                case 2:
                    _name = bs.readString();
                    break;
                case 3:
                    _designMode = bs.readBool();
                    break;
                case 4:
                    _version = bs.readInt();
                    break;
                case 5:
                    _persistentState = PersistentState.fromValue(bs.readByte());
                    break;
                case 7:
                    _originalName = bs.readString();
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
