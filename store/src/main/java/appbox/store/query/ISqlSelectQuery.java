package appbox.store.query;

import appbox.store.expressions.SqlSelectItemExpression;

import java.util.Collection;

public interface ISqlSelectQuery extends ISqlQuery {
    enum QueryPurpose {
        None, Count, ToScalar, ToDynamic, ToTreeList, ToSingle, ToList, ToTreeNodePath;
    }

    QueryPurpose getPurpose();

    Collection<SqlSelectItemExpression> getSelects();

}
