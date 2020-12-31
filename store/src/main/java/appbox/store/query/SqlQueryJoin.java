package appbox.store.query;

import appbox.expressions.EntityPathExpression;
import appbox.expressions.EntityExpression;

public final class SqlQueryJoin extends SqlQueryBase implements ISqlQueryJoin {

    public final EntityExpression t;

    public SqlQueryJoin(long modelId) {
        t = new EntityExpression(modelId, this);
    }

    public EntityPathExpression m(String name) {
        return t.m(name);
    }

}
