package appbox.store.query;

import appbox.expressions.Expression;

import java.util.ArrayList;
import java.util.List;

public abstract class SqlQueryBase {

    public String aliasName; //仅用于构建sql时暂存名称

    private List<SqlJoin> _joins;

    public boolean hasJoins() {
        return _joins != null && _joins.size() > 0;
    }

    public List<SqlJoin> getJoins() {
        if (_joins == null)
            _joins = new ArrayList<>();
        return _joins;
    }

    public ISqlQueryJoin join(SqlJoin.JoinType type, ISqlQueryJoin target, Expression onCondition) {
        if (target == null || onCondition == null)
            throw new IllegalArgumentException();

        getJoins().add(new SqlJoin(target, type, onCondition));
        return target;
    }

}
