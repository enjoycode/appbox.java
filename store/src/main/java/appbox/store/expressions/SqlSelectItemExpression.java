package appbox.store.expressions;

import appbox.expressions.EntityFieldExpression;
import appbox.expressions.Expression;
import appbox.expressions.ExpressionType;
import appbox.store.query.ISqlSelectQuery;

public final class SqlSelectItemExpression extends Expression {

    public String          aliasName;
    public ISqlSelectQuery owner;
    public Expression      expression;

    public SqlSelectItemExpression(Expression expression) {
        this.expression = expression;
        switch (expression.getType()) {
            case FieldExpression:
                aliasName = ((EntityFieldExpression) expression).name;
                break;
            case SelectItemExpression:
                aliasName = ((SqlSelectItemExpression) expression).aliasName;
                break;
            default:
                aliasName = "unnamed";
                break;
        }
    }

    public SqlSelectItemExpression(Expression expression, String aliasName) {
        this.expression = expression;
        this.aliasName  = aliasName;
    }

    @Override
    public ExpressionType getType() { return ExpressionType.SelectItemExpression;}

    @Override
    public void toCode(StringBuilder sb, String preTabs) {
        expression.toCode(sb, preTabs);
        sb.append(" AS ");
        sb.append(aliasName);
    }
}
