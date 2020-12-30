package appbox.store.query;

import appbox.expressions.Expression;
import appbox.expressions.ExpressionType;

import java.util.ArrayList;
import java.util.List;

/** 用于将[SqlFromQuery]或[SqlQuery]包装为子查询表达式 */
public final class SqlSubQuery extends Expression implements ISqlQueryJoin {

    private      List<SqlJoin>   _joins;
    public final ISqlSelectQuery target;

    public SqlSubQuery(ISqlSelectQuery target) {
        this.target = target;
    }

    @Override
    public ExpressionType getType() {
        return ExpressionType.SubQueryExpression;
    }

    @Override
    public void toCode(StringBuilder sb, String preTabs) {
        sb.append("SubQuery(");
        sb.append(target.toString());
        sb.append(")");
    }

    //region ====ISqlQueryJoin====
    @Override
    public boolean hasJoins() {
        return _joins != null && _joins.size() > 0;
    }

    @Override
    public List<SqlJoin> getJoins() {
        if (_joins == null)
            _joins = new ArrayList<>();
        return _joins;
    }

    @Override
    public ISqlQueryJoin join(SqlJoin.JoinType type, ISqlQueryJoin target, Expression onCondition) {
        if (target == null || onCondition == null)
            throw new IllegalArgumentException();

        getJoins().add(new SqlJoin(target, type, onCondition));
        return target;
    }
    //endregion

}
