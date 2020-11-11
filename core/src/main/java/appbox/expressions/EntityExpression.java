package appbox.expressions;

import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;

public final class EntityExpression extends EntityBaseExpression {

    /** 用于查询时的别名，不用序列化 */
    protected    String aliasName;
    public final long   modelId;
    private      Object _user;

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
    public void writeTo(BinSerializer bs) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {
        throw new UnsupportedOperationException();
    }
}
