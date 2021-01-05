package appbox.store.query;

import appbox.expressions.EntityPathExpression;
import appbox.expressions.Expression;

import java.util.List;

public interface ISqlQueryJoin {

    boolean hasJoins();

    List<SqlJoin> getJoins();

    ISqlQueryJoin join(SqlJoin.JoinType type, ISqlQueryJoin target, Expression onCondition);

    EntityPathExpression m(String name);

    //default ISqlQueryJoin leftJoin(ISqlQueryJoin target, Expression onCondition) {
    //    return join(SqlJoin.JoinType.Left, target, onCondition);
    //}
    //
    //default ISqlQueryJoin innerJoin(ISqlQueryJoin target, Expression onCondition) {
    //    return join(SqlJoin.JoinType.Inner, target, onCondition);
    //}
    //
    //default ISqlQueryJoin rightJoin(ISqlQueryJoin target, Expression onCondition) {
    //    return join(SqlJoin.JoinType.Right, target, onCondition);
    //}
    //
    //default ISqlQueryJoin fullJoin(ISqlQueryJoin target, Expression onCondition) {
    //    return join(SqlJoin.JoinType.Full, target, onCondition);
    //}

}
