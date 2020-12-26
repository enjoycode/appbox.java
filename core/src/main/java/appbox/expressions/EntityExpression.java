package appbox.expressions;

import appbox.model.EntityModel;
import appbox.model.entity.EntityRefModel;
import appbox.runtime.RuntimeContext;

import java.util.HashMap;

public final class EntityExpression extends EntityBaseExpression {

    /** 用于查询时的别名，不用序列化 */
    protected    String aliasName;
    public final long   modelId;
    private      Object _user;

    private EntityModel                           _model; //only for cache
    private HashMap<String, EntityBaseExpression> _cache; //only for cache

    /** New Root EntityExpression */
    public EntityExpression(long modelId, Object user) {
        super(null, null);
        this.modelId = modelId;
        this._user   = user;
    }

    /** New EntityRefModel's EntityExpression */
    public EntityExpression(String name, long modelId, EntityExpression owner) {
        super(name, owner);
        this.modelId = modelId;
    }

    public Object getUser() {
        return owner == null ? _user : owner._user;
    }

    public void setUser(Object user) {
        if (owner == null)
            _user = user;
        else
            throw new UnsupportedOperationException();
    }

    public String getAliasName() { return aliasName; }

    public void setAliasName(String value) { aliasName = value; }

    @Override
    public ExpressionType getType() {
        return ExpressionType.EntityExpression;
    }

    @Override
    public void toCode(StringBuilder sb, String preTabs) {
        if (owner == null) {
            sb.append(aliasName == null ? 't' : aliasName);
        } else {
            owner.toCode(sb, preTabs);
            sb.append('.');
            sb.append(name);
        }
    }

    /** 根据名称获取成员表达式 */
    @Override
    public EntityBaseExpression m(String name) {
        //先尝试从缓存中获取
        EntityBaseExpression exp = _cache == null ? null : _cache.get(name);
        if (exp != null)
            return exp;

        if (_cache == null)
            _cache = new HashMap<>();
        if (_model == null)
            _model = RuntimeContext.current().getModel(modelId);

        var m = _model.tryGetMember(name);
        if (m == null)
            throw new RuntimeException(String.format("Can't find member: %s.%s", _model.name(), name));

        EntityBaseExpression member = null;
        switch (m.type()) {
            case DataField:
                member = new EntityFieldExpression(name, this);
                _cache.put(name, member);
                break;
            case EntityRef:
                var rm = (EntityRefModel) m;
                if (!rm.isAggregationRef()) {
                    member = new EntityExpression(name, rm.getRefModelIds().get(0), this);
                } else {
                    throw new RuntimeException("未实现");
                }
            default:
                throw new RuntimeException("未实现");
        }
        return member;
    }
}
