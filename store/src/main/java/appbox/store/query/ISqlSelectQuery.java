package appbox.store.query;

import appbox.expressions.Expression;
import appbox.store.expressions.SqlSelectItem;

import java.util.List;

public interface ISqlSelectQuery extends ISqlQuery {
    enum QueryPurpose {
        None, Count, ToScalar, ToDynamic, ToTreeList, ToSingle, ToList, ToTreeNodePath;
    }

    QueryPurpose getPurpose();

    int getSkipSize();

    int getTakeSize();

    List<SqlOrderBy> getOrderBy();

    default boolean hasOrderBy() {
        return getOrderBy() != null && getOrderBy().size() > 0;
    }

    List<SqlSelectItem> getSelects();

    List<SqlSelectItem> getGroupBy();

    Expression getHavingFilter();

}
