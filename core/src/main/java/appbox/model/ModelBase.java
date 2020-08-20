package appbox.model;

import appbox.data.PersistentState;
import appbox.utils.IdUtil;

/**
 * 模型基类，实例分为设计时与运行时
 */
public abstract class ModelBase {
    private long            _id;
    private String          _name;
    private String          _originalName;
    private boolean         _designMode;
    private int             _version;
    private PersistentState _persistentState;

    /**
     * only for Serialization
     */
    public ModelBase() {
    }

    public ModelBase(long id, String name) {
        _designMode = true;
        _id         = id;
        _name       = name;
    }

    //region ====Properties====
    public ModelLayer modelLayer() {
        return ModelLayer.getByValue((byte) (_id & IdUtil.MODELID_LAYER_MASK));
    }

    public String name() {
        return _name;
    }

    public abstract ModelType modelType();

    public boolean designMode() {
        return _designMode;
    }

    public PersistentState persistentState() {
        return _persistentState;
    }
    //endregion

    //region ====Design Methods====
    public void checkDesignMode() throws RuntimeException {
        if (!designMode()) {
            throw new RuntimeException();
        }
    }

    public void onPropertyChanged() {
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
    //endregion
}