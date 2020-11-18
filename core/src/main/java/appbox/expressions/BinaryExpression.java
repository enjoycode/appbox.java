package appbox.expressions;

import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;

public final class BinaryExpression extends Expression {
    public enum BinaryOperatorType {
        BitwiseAnd(7),
        BitwiseOr(8),
        BitwiseXor(9),
        Divide(10),
        Equal(0),
        Greater(2),
        GreaterOrEqual(5),
        In(17),
        NotIn(22),
        Is(16),
        IsNot(15),
        Less(3),
        LessOrEqual(4),
        Like(6),
        Minus(14),
        Modulo(11),
        Multiply(12),
        NotEqual(1),
        Plus(13),
        As(18),
        AndAlso(19),
        OrElse(20),
        Assign(21);

        public final byte value;

        BinaryOperatorType(int value) {
            this.value = (byte) value;
        }

        public static BinaryExpression.BinaryOperatorType fromValue(byte v) {
            for (BinaryExpression.BinaryOperatorType item : BinaryExpression.BinaryOperatorType.values()) {
                if (item.value == v) {
                    return item;
                }
            }
            throw new RuntimeException("Unknown value: " + v);
        }
    }

    public final Expression         leftOperand;
    public final BinaryOperatorType binaryType;
    public final Expression         rightOperand;

    public BinaryExpression(Expression left, Expression right, BinaryOperatorType type) {
        this.leftOperand  = left;
        this.rightOperand = right;
        this.binaryType   = type;
    }

    @Override
    public ExpressionType getType() {
        return ExpressionType.BinaryExpression;
    }

    @Override
    public void toCode(StringBuilder sb, String preTabs) {
        //TODO:判断In,Like等特殊语法进行方法转换，否则解析器无法解析
        throw new RuntimeException("未实现");
    }

    @Override
    public void writeTo(BinSerializer bs) {
        bs.serialize(leftOperand);
        bs.writeByte(binaryType.value);
        bs.serialize(rightOperand);
    }

    @Override
    public void readFrom(BinDeserializer bs) {
        throw new UnsupportedOperationException();
    }
}
