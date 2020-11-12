package appbox.expressions;

public abstract class EntityBaseExpression extends Expression {

    /**
     * 分以下几种情况：
     * 1、如果为EntityExpression
     * 1.1 如果为Root EntityExpression，Name及Owner属性皆为null
     * 1.2 如果为Ref EntityExpression，Name即属性名称
     * 2、如果为FieldExpression，Name为属性名称
     */
    public final String name;

    public final EntityExpression owner;

    public EntityBaseExpression(String name, EntityExpression owner) {
        this.name  = name;
        this.owner = owner;
    }

    public EntityBaseExpression m(String name) {
        throw new UnsupportedOperationException();
    }

}
