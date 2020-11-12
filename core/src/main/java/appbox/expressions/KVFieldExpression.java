package appbox.expressions;

import appbox.model.entity.DataFieldModel;
import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;

public final class KVFieldExpression extends Expression {
    public final short                        fieldId;
    public final DataFieldModel.DataFieldType fieldType;

    public KVFieldExpression(short id, DataFieldModel.DataFieldType type) {
        fieldId   = id;
        fieldType = type;
    }

    @Override
    public ExpressionType getType() {
        return ExpressionType.KVFieldExpression;
    }

    @Override
    public void toCode(StringBuilder sb, String preTabs) {
        sb.append(fieldId);
        sb.append('[');
        sb.append(fieldType.toString());
        sb.append(']');
    }

    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        bs.writeShort(fieldId);
        bs.writeByte(fieldType.value);
    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {
        throw new UnsupportedOperationException();
    }
}
