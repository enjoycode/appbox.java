package appbox.expressions;

import appbox.serialization.IBinSerializable;
import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;
import org.apache.commons.math3.fitting.leastsquares.EvaluationRmsChecker;

public abstract class Expression implements IBinSerializable/*TODO:移至需要的实现*/ {
    public abstract ExpressionType getType();

    private BinaryExpression makeBinary(Object value, BinaryExpression.BinaryOperatorType op) {
        return value instanceof Expression ?
                new BinaryExpression(this, (Expression) value, op) :
                new BinaryExpression(this, new PrimitiveExpression(value), op);
    }

    public BinaryExpression set(Object value) {
        return makeBinary(value, BinaryExpression.BinaryOperatorType.Assign);
    }

    public BinaryExpression eq(Object value) {
        return makeBinary(value, BinaryExpression.BinaryOperatorType.Equal);
    }

    public BinaryExpression ne(Object value) {
        return makeBinary(value, BinaryExpression.BinaryOperatorType.NotEqual);
    }

    public BinaryExpression lt(Object value) {
        return makeBinary(value, BinaryExpression.BinaryOperatorType.Less);
    }

    public BinaryExpression le(Object value) {
        return makeBinary(value, BinaryExpression.BinaryOperatorType.LessOrEqual);
    }

    public BinaryExpression gt(Object value) {
        return makeBinary(value, BinaryExpression.BinaryOperatorType.Greater);
    }

    public BinaryExpression ge(Object value) {
        return makeBinary(value, BinaryExpression.BinaryOperatorType.GreaterOrEqual);
    }

    public BinaryExpression plus(Object value) {
        return makeBinary(value, BinaryExpression.BinaryOperatorType.Plus);
    }

    public BinaryExpression minus(Object value) {
        return makeBinary(value, BinaryExpression.BinaryOperatorType.Minus);
    }

    public BinaryExpression and(Expression right) {
        return new BinaryExpression(this, right, BinaryExpression.BinaryOperatorType.AndAlso);
    }

    public BinaryExpression or(Expression right) {
        return new BinaryExpression(this, right, BinaryExpression.BinaryOperatorType.OrElse);
    }

    @Override
    public String toString() {
        var sb = new StringBuilder(20);
        toCode(sb, null);
        return sb.toString();
    }

    public abstract void toCode(StringBuilder sb, String preTabs);

    @Override
    public void writeTo(IOutputStream bs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readFrom(IInputStream bs) {
        throw new UnsupportedOperationException();
    }
}
