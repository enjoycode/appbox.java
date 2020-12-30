package appbox.store.query;

import appbox.expressions.EntityBaseExpression;
import appbox.expressions.EntityExpression;

public final class SqlQueryJoin extends SqlQueryBase implements ISqlQueryJoin {

    public final EntityExpression t;

    public SqlQueryJoin(long modelId) {
        t = new EntityExpression(modelId, this);
    }

    public EntityBaseExpression m(String name) {
        return t.m(name);
    }

}
