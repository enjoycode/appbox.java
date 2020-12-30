package appbox.store;

import appbox.expressions.BinaryExpression;
import appbox.expressions.Expression;
import appbox.expressions.ExpressionType;
import appbox.expressions.PrimitiveExpression;
import appbox.store.query.SqlSubQuery;

import java.util.Collection;

public final class DbFunc extends Expression {
    public final String       name;
    public final Expression[] arguments;

    private DbFunc(String name, Expression... arguments) {
        this.name      = name;
        this.arguments = arguments;
    }

    @Override
    public ExpressionType getType() {
        return ExpressionType.DbFuncExpression;
    }

    @Override
    public void toCode(StringBuilder sb, String preTabs) {
        sb.append(name);
        sb.append("()");
    }

    public static DbFunc sum(Expression field) {
        return new DbFunc("Sum", field);
    }

    public static DbFunc avg(Expression field) {
        return new DbFunc("Avg", field);
    }

    public static DbFunc max(Expression field) {
        return new DbFunc("Max", field);
    }

    public static DbFunc min(Expression field) {
        return new DbFunc("Min", field);
    }

    public static BinaryExpression in(Expression field, SqlSubQuery subQuery) {
        return new BinaryExpression(field, subQuery, BinaryExpression.BinaryOperatorType.In);
    }

    public static BinaryExpression in(Expression field, Collection<?> collection) {
        return new BinaryExpression(field, new PrimitiveExpression(collection), BinaryExpression.BinaryOperatorType.In);
    }

}
