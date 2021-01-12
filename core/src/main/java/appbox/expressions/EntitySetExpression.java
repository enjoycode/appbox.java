package appbox.expressions;

public final class EntitySetExpression extends EntityPathExpression {

    private final long             _setModelId; //用于thenInclude
    private       EntityExpression _root; //用于thenInclude

    protected EntitySetExpression(String name, EntityExpression owner, long setModelId) {
        super(name, owner);
        _setModelId = setModelId;
    }

    public EntityExpression rootEntityExpression() {
        if (_root == null)
            _root = new EntityExpression(_setModelId, null /*必须null,后面设置*/);
        return _root;
    }

    @Override
    public ExpressionType getType() {
        return ExpressionType.EntitySetExpression;
    }

    @Override
    public void toCode(StringBuilder sb, String preTabs) {
        owner.toCode(sb, preTabs);
        sb.append('.');
        sb.append(name);
    }
}
