package appbox.store.query;

import appbox.expressions.*;
import appbox.model.EntityModel;
import appbox.model.entity.EntityRefModel;
import appbox.runtime.RuntimeContext;
import appbox.store.expressions.SqlSelectItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** 用于Eager或Explicit加载实体Navigation属性 */
public final class SqlIncluder {

    //Only EntityExpression | EntitySetExpression | SqlSelectItemExpression
    public final Expression  expression;
    //上级，根级为null
    public final SqlIncluder parent;

    private List<SqlIncluder> _childs;

    protected SqlIncluder(EntityExpression root) {
        if (root.owner != null)
            throw new IllegalArgumentException("Must be root");
        expression = root;
        parent     = null;
    }

    private SqlIncluder(SqlIncluder parent, Expression expression) {
        if (parent == null)
            throw new IllegalArgumentException("Parent is null");
        this.expression = expression;
        this.parent     = parent;
    }

    //region ====Include Methods====
    private SqlIncluder getRoot() {
        return parent == null ? this : parent.getRoot();
    }

    private EntityPathExpression memberExpression() {
        if (expression.getType() == ExpressionType.EntityExpression
                || expression.getType() == ExpressionType.EntitySetExpression)
            return (EntityPathExpression) expression;
        return (EntityPathExpression) ((SqlSelectItem) expression).expression;
    }

    private SqlIncluder includeInternal(EntityPathExpression member, String alias) {
        //检查当前是否EntityField，是则不再允许include其他
        if (memberExpression().getType() == ExpressionType.FieldExpression)
            throw new UnsupportedOperationException("EntityField can't include others");
        if (member.getType() == ExpressionType.FieldExpression) {
            //可以include多个层级，eg:t.Customer.Region.Name
            if (member.owner == memberExpression())
                throw new UnsupportedOperationException("Can't include field");
            //判断alias空，是则自动生成eg:t.Customer.Region.Name => CustomerRegionName
            if (alias == null || alias.isEmpty())
                alias = member.getFieldAlias();
            //TODO:判断重复
            if (_childs == null)
                _childs = new ArrayList<>();
            var res = new SqlIncluder(this, new SqlSelectItem(member, alias));
            _childs.add(res);
            return res;
        } else { //EntityRef or EntitySet
            if (member.getType() != ExpressionType.EntityExpression
                    && member.getType() != ExpressionType.EntitySetExpression)
                throw new UnsupportedOperationException("None EntityRef nor EntitySet");
            if (_childs == null) {
                var res = new SqlIncluder(this, member);
                _childs = new ArrayList<>();
                _childs.add(res);
                return res;
            }

            var found = _childs.stream().filter(t -> t.expression.getType() == member.getType()
                    && t.memberExpression().name.equals(member.name)).findAny();
            if (found.isPresent())
                return found.get();
            var res = new SqlIncluder(this, member);
            _childs.add(res);
            return res;
        }
    }

    public SqlIncluder include(Function<EntityExpression, EntityPathExpression> select) {
        var root = getRoot();
        return root.includeInternal(select.apply((EntityExpression) root.expression), null);
    }

    public SqlIncluder thenInclude(Function<EntityExpression, EntityPathExpression> select) {
        if (expression.getType() == ExpressionType.EntitySetExpression) {
            return includeInternal(select.apply(((EntitySetExpression) expression).rootEntityExpression()), null);
        }
        return includeInternal(select.apply((EntityExpression) expression), null);
    }
    //endregion

    //region ====Runtime Methods====
    protected void addSelects(SqlQuery<?> query, EntityModel model, String path) {
        if (_childs == null)
            return;
        for (var child : _childs) {
            child.loopAddSelects(query, model, path);
        }
    }

    private void loopAddSelects(SqlQuery<?> query, EntityModel model, String path) {
        if (expression.getType() == ExpressionType.EntityExpression) {
            var exp = (EntityExpression) expression;
            var mm  = (EntityRefModel) model.getMember(exp.name);
            if (mm.isAggregationRef()) //TODO:聚合引用转换为Case表达式
                throw new RuntimeException("未实现");
            //注意替换入参支持多级
            model = RuntimeContext.current().getModel(mm.getRefModelIds().get(0));
            path  = path == null ? exp.name : String.format("%s.%s", path, exp.name);
            SqlQuery.addAllSelects(query, model, exp, path);
        } else if (expression.getType() == ExpressionType.SelectItemExpression) {
            query.addSelect((SqlSelectItem) expression);
        } else {
            return;
        }

        if (_childs == null)
            return;
        for (var child : _childs) {
            child.loopAddSelects(query, model, path);
        }
    }
    //endregion

}
