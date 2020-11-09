package appbox.expressions;

import appbox.serialization.IBinSerializable;

public abstract class Expression implements IBinSerializable {
    public abstract ExpressionType getType();

    public BinaryExpression equalsTo(Object value) {
        return new BinaryExpression(this, new PrimitiveExpression(value), BinaryExpression.BinaryOperatorType.Equal);
    }
}
