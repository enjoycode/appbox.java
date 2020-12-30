package appbox.store.query;

import appbox.expressions.Expression;

public final class SqlJoin {

    public final ISqlQueryJoin right;
    public final Expression    onCondition;
    public final JoinType      joinType;

    public SqlJoin(ISqlQueryJoin right, JoinType type, Expression onCondition) {
        this.right       = right;
        this.joinType    = type;
        this.onCondition = onCondition;
    }

    public enum JoinType {
        Inner, Left, Right, Full;
    }

}
