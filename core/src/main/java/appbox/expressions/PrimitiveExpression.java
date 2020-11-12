package appbox.expressions;

import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;

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
    public void writeTo(BinSerializer bs) throws Exception {
        bs.serialize(value);
    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {
        throw new UnsupportedOperationException();
    }
}
