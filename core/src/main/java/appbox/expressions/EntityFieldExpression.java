package appbox.expressions;

public final class EntityFieldExpression extends EntityBaseExpression {


    public EntityFieldExpression(String name, EntityExpression owner) {
        super(name, owner);
    }

    @Override
    public ExpressionType getType() { return ExpressionType.FieldExpression;}
}
