package appbox.expressions;

/** 描述实体路径的表达式，eg: t.City */
public abstract class EntityPathExpression extends Expression {

    /**
     * 分以下几种情况：
     * 1、如果为EntityExpression
     * 1.1 如果为Root EntityExpression，Name及Owner属性皆为null
     * 1.2 如果为Ref EntityExpression，Name即属性名称
     * 2、如果为FieldExpression，Name为属性名称
     */
    public final String name;

    public final EntityExpression owner;

    public EntityPathExpression(String name, EntityExpression owner) {
        this.name  = name;
        this.owner = owner;
    }

    public EntityPathExpression m(String name) {
        throw new UnsupportedOperationException();
    }

    /** Customer.Name -> CustomerName */
    public String getFieldAlias() {
        return owner == null ? name : String.format("%s.%s", owner.getFieldAlias(), name);
    }

}
