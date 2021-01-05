package appbox.store.query;

import appbox.expressions.EntityPathExpression;
import appbox.expressions.EntityExpression;
import appbox.expressions.Expression;

import java.util.function.BiFunction;

public final class SqlQueryJoin extends SqlQueryBase implements ISqlQueryJoin {

    public final EntityExpression t;

    public SqlQueryJoin(long modelId) {
        t = new EntityExpression(modelId, this);
    }

    @Override
    public EntityPathExpression m(String name) {
        return t.m(name);
    }

    //region ====Join====
    public ISqlQueryJoin leftJoin(ISqlQueryJoin target, BiFunction<ISqlQueryJoin, ISqlQueryJoin, Expression> onCondition) {
        return join(SqlJoin.JoinType.Left, target, onCondition.apply(this, target));
    }

    public ISqlQueryJoin rightJoin(ISqlQueryJoin target, BiFunction<ISqlQueryJoin, ISqlQueryJoin, Expression> onCondition) {
        return join(SqlJoin.JoinType.Right, target, onCondition.apply(this, target));
    }

    public ISqlQueryJoin innerJoin(ISqlQueryJoin target, BiFunction<ISqlQueryJoin, ISqlQueryJoin, Expression> onCondition) {
        return join(SqlJoin.JoinType.Inner, target, onCondition.apply(this, target));
    }

    public ISqlQueryJoin fullJoin(ISqlQueryJoin target, BiFunction<ISqlQueryJoin, ISqlQueryJoin, Expression> onCondition) {
        return join(SqlJoin.JoinType.Full, target, onCondition.apply(this, target));
    }
    //endregion

}
