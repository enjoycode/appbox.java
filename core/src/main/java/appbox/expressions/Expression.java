package appbox.expressions;

import appbox.serialization.IBinSerializable;
import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;

public abstract class Expression implements IBinSerializable/*TODO:移至需要的实现*/ {
    public abstract ExpressionType getType();

    private BinaryExpression makeBinary(Object value, BinaryExpression.BinaryOperatorType op) {
        return value instanceof Expression ?
                new BinaryExpression(this, (Expression) value, op) :
                new BinaryExpression(this, new PrimitiveExpression(value), op);
    }

    public final BinaryExpression set(Object value) {
        return makeBinary(value, BinaryExpression.BinaryOperatorType.Assign);
    }

    public final BinaryExpression eq(Object value) {
        return makeBinary(value, BinaryExpression.BinaryOperatorType.Equal);
    }

    public final BinaryExpression ne(Object value) {
        return makeBinary(value, BinaryExpression.BinaryOperatorType.NotEqual);
    }

    public final BinaryExpression lt(Object value) {
        return makeBinary(value, BinaryExpression.BinaryOperatorType.Less);
    }

    public final BinaryExpression le(Object value) {
        return makeBinary(value, BinaryExpression.BinaryOperatorType.LessOrEqual);
    }

    public final BinaryExpression gt(Object value) {
        return makeBinary(value, BinaryExpression.BinaryOperatorType.Greater);
    }

    public final BinaryExpression ge(Object value) {
        return makeBinary(value, BinaryExpression.BinaryOperatorType.GreaterOrEqual);
    }

    public final BinaryExpression plus(Object value) {
        return makeBinary(value, BinaryExpression.BinaryOperatorType.Plus);
    }

    public final BinaryExpression minus(Object value) {
        return makeBinary(value, BinaryExpression.BinaryOperatorType.Minus);
    }

    public final BinaryExpression times(Object value) {
        return makeBinary(value, BinaryExpression.BinaryOperatorType.Multiply);
    }

    public final BinaryExpression div(Object value) {
        return makeBinary(value, BinaryExpression.BinaryOperatorType.Divide);
    }

    public final BinaryExpression and(Expression right) {
        return new BinaryExpression(this, right, BinaryExpression.BinaryOperatorType.AndAlso);
    }

    public final BinaryExpression or(Expression right) {
        return new BinaryExpression(this, right, BinaryExpression.BinaryOperatorType.OrElse);
    }

    public final BinaryExpression contains(CharSequence right) {
        return new BinaryExpression(this, new PrimitiveExpression(right), BinaryExpression.BinaryOperatorType.Like);
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
