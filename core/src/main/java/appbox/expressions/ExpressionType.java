package appbox.expressions;

public enum ExpressionType {
    EntityExpression(0),
    FieldExpression(1),    //TODO: rename
    EntitySetExpression(2),
    //AggregationRefFieldExpression(3),
    //EnumItemExpression(4),
    KVFieldExpression(5),
    PrimitiveExpression(6),

    BinaryExpression(7),
    GroupExpression(8),

    //BlockExpression(9),
    //EventAction(10),
    //AssignmentExpression(11),
    //IdentifierExpression(12),
    //MemberAccessExpression(13),
    //IfStatementExpression(14),
    //LocalDeclaration(15),
    //TypeExpression(16),

    SubQueryExpression(17),   //TOOD: rename
    SelectItemExpression(18), //TODO: rename

    //InvocationExpression(19),
    DbFuncExpression(20),
    DbParameterExpression(21);
    //LambdaExpression(22),

    //FormCreationExpression(23),
    //ArrayCreationExpression(24),

    public final byte value;

    ExpressionType(int value) {
        this.value = (byte) value;
    }

    public static ExpressionType fromValue(byte v) {
        for (ExpressionType item : ExpressionType.values()) {
            if (item.value == v) {
                return item;
            }
        }
        throw new RuntimeException("Unknown value: " + v);
    }
}
