package appbox.expressions;

import appbox.serialization.IBinSerializable;
import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;
import org.apache.commons.math3.fitting.leastsquares.EvaluationRmsChecker;

public abstract class Expression implements IBinSerializable/*TODO:移至需要的实现*/ {
    public abstract ExpressionType getType();

    public BinaryExpression set(Object value) {
        var op = BinaryExpression.BinaryOperatorType.Assign;
        return value instanceof Expression ?
                new BinaryExpression(this, (Expression) value, op) :
                new BinaryExpression(this, new PrimitiveExpression(value), op);
    }

    public BinaryExpression eq(Object value) {
        var op = BinaryExpression.BinaryOperatorType.Equal;
        return value instanceof Expression ?
                new BinaryExpression(this, (Expression) value, op) :
                new BinaryExpression(this, new PrimitiveExpression(value), op);
    }

    public BinaryExpression ne(Object value) {
        var op = BinaryExpression.BinaryOperatorType.NotEqual;
        return value instanceof Expression ?
                new BinaryExpression(this, (Expression) value, op) :
                new BinaryExpression(this, new PrimitiveExpression(value), op);
    }

    public BinaryExpression lt(Object value) {
        var op = BinaryExpression.BinaryOperatorType.Less;
        return value instanceof Expression ?
                new BinaryExpression(this, (Expression) value, op) :
                new BinaryExpression(this, new PrimitiveExpression(value), op);
    }

    public BinaryExpression le(Object value) {
        var op = BinaryExpression.BinaryOperatorType.LessOrEqual;
        return value instanceof Expression ?
                new BinaryExpression(this, (Expression) value, op) :
                new BinaryExpression(this, new PrimitiveExpression(value), op);
    }

    public BinaryExpression gt(Object value) {
        var op = BinaryExpression.BinaryOperatorType.Greater;
        return value instanceof Expression ?
                new BinaryExpression(this, (Expression) value, op) :
                new BinaryExpression(this, new PrimitiveExpression(value), op);
    }

    public BinaryExpression ge(Object value) {
        var op = BinaryExpression.BinaryOperatorType.GreaterOrEqual;
        return value instanceof Expression ?
                new BinaryExpression(this, (Expression) value, op) :
                new BinaryExpression(this, new PrimitiveExpression(value), op);
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
