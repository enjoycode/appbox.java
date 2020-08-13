package appbox.core.model;

import appbox.core.utils.IdUtil;

/**
 * 模型基类，实例分为设计时与运行时
 */
public abstract class ModelBase {
    private long    _id;
    private String  _name;
    private String  _originalName;
    private boolean _designMode;
    private int     _version;

    public ModelLayer getModelLayer() {
        return ModelLayer.getByValue((byte) (_id & IdUtil.MODELID_LAYER_MASK));
    }

    public String getName() {
        return _name;
    }

    public abstract ModelType getModelType();
}