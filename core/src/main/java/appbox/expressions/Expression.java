package appbox.expressions;

import appbox.serialization.IBinSerializable;
import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;

public abstract class Expression implements IBinSerializable/*TODO:移至需要的实现*/ {
    public abstract ExpressionType getType();

    public BinaryExpression eq(Expression value) {
        return new BinaryExpression(this, value, BinaryExpression.BinaryOperatorType.Equal);
    }

    public BinaryExpression eq(Object value) {
        return new BinaryExpression(this, new PrimitiveExpression(value), BinaryExpression.BinaryOperatorType.Equal);
    }

    public BinaryExpression le(Object value) {
        return new BinaryExpression(this, new PrimitiveExpression(value), BinaryExpression.BinaryOperatorType.LessOrEqual);
    }

    public BinaryExpression gt(Object value) {
        return new BinaryExpression(this, new PrimitiveExpression(value), BinaryExpression.BinaryOperatorType.Greater);
    }

    public BinaryExpression ge(Object value) {
        return new BinaryExpression(this, new PrimitiveExpression(value), BinaryExpression.BinaryOperatorType.GreaterOrEqual);
    }

    public BinaryExpression and(Expression right) {
        return new BinaryExpression(this, right, BinaryExpression.BinaryOperatorType.AndAlso);
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
