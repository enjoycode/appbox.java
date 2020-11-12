package appbox.expressions;

public final class EntityFieldExpression extends EntityBaseExpression {

    public EntityFieldExpression(String name, EntityExpression owner) {
        super(name, owner);
    }

    @Override
    public ExpressionType getType() { return ExpressionType.FieldExpression;}

    @Override
    public void toCode(StringBuilder sb, String preTabs) {
        owner.toCode(sb, preTabs);
        sb.append('.');
        sb.append(name);
    }
}
