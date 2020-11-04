package appbox.data;

import appbox.model.EntityModel;

public final class Entity {

    private final EntityId    _id = new EntityId();
    private       EntityModel _model; //不为null,新建或反序列化时设置

    public EntityId id() {
        return _id;
    }

    public EntityModel model() {
        return _model;
    }

}
