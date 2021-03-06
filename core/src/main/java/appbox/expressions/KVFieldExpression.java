package appbox.expressions;

import appbox.model.entity.DataFieldModel;
import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;

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
    public void writeTo(IOutputStream bs) {
        bs.writeShort(fieldId);
        bs.writeByte(fieldType.value);
    }

    @Override
    public void readFrom(IInputStream bs) {
        throw new UnsupportedOperationException();
    }
}
