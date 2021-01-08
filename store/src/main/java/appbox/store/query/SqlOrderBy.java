package appbox.store.query;

import appbox.expressions.Expression;
import appbox.expressions.ExpressionType;

public final class SqlOrderBy {

    public final Expression expression;
    public final boolean    desc;

    public SqlOrderBy(Expression expression) {
        checkExpression(expression);
        this.expression = expression;
        this.desc       = false;
    }

    public SqlOrderBy(Expression expression, boolean desc) {
        checkExpression(expression);
        this.expression = expression;
        this.desc       = desc;
    }

    private static void checkExpression(Expression expression) {
        if (expression.getType() != ExpressionType.FieldExpression
                && expression.getType() != ExpressionType.SelectItemExpression) {
            throw new RuntimeException("Only for FieldExpression or SelectItemExpression");
        }
    }

}
