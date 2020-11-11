package appbox.expressions;

import appbox.model.EntityModel;
import appbox.runtime.RuntimeContext;

public final class EntityExpression extends EntityBaseExpression {

    /** 用于查询时的别名，不用序列化 */
    protected    String aliasName;
    public final long   modelId;
    private      Object _user;

    private EntityModel _model; //only for cache

    /** New Root EntityExpression */
    public EntityExpression(long modelId, Object user) {
        super(null, null);
        this.modelId = modelId;
        this._user   = user;
    }

    /** New EntityRefModel's EntityExpression */
    public EntityExpression(String name, long modelId, EntityExpression owner) {
        super(name, owner);
        this.modelId = modelId;
    }

    public Object getUser() {
        return owner == null ? _user : owner._user;
    }

    public void setUser(Object user) {
        if (owner == null)
            _user = user;
        else
            throw new UnsupportedOperationException();
    }

    @Override
    public ExpressionType getType() {
        return ExpressionType.EntityExpression;
    }

    @Override
    public EntityBaseExpression get(String name) {
        //TODO: use cache
        if (_model == null)
            _model = RuntimeContext.current().getModel(modelId);

        var m = _model.tryGetMember(name);
        if (m == null)
            throw new RuntimeException(String.format("Can't find member: %s.%s", _model.name(), name));
        switch (m.type()) {
            case DataField:
                return new EntityFieldExpression(name, this);
            default:
                throw new RuntimeException("未实现");
        }
    }
}
