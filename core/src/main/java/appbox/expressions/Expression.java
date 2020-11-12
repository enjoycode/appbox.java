package appbox.expressions;

import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.serialization.IBinSerializable;

public abstract class Expression implements IBinSerializable/*TODO:移至需要的实现*/ {
    public abstract ExpressionType getType();

    public BinaryExpression eq(Object value) {
        return new BinaryExpression(this, new PrimitiveExpression(value), BinaryExpression.BinaryOperatorType.Equal);
    }

    @Override
    public String toString() {
        var sb = new StringBuilder(20);
        toCode(sb, null);
        return sb.toString();
    }

    public abstract void toCode(StringBuilder sb, String preTabs);

    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {
        throw new UnsupportedOperationException();
    }
}
