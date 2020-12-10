package appbox.expressions;

import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;

public final class PrimitiveExpression extends Expression {
    public final Object value;

    public PrimitiveExpression(Object value) {
        this.value = value;
    }

    @Override
    public ExpressionType getType() {
        return ExpressionType.PrimitiveExpression;
    }

    @Override
    public void toCode(StringBuilder sb, String preTabs) {
        if (value == null) {
            sb.append("null"); return;
        }
        sb.append(value.toString());
    }

    @Override
    public void writeTo(IOutputStream bs) {
        bs.serialize(value);
    }

    @Override
    public void readFrom(IInputStream bs) {
        throw new UnsupportedOperationException();
    }
}
