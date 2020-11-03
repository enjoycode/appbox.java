package appbox.expressions;

import appbox.model.entity.DataFieldModel;

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
}
