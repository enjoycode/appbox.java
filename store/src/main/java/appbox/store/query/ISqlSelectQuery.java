package appbox.store.query;

import appbox.store.expressions.SqlSelectItemExpression;

import java.util.List;

public interface ISqlSelectQuery extends ISqlQuery {
    enum QueryPurpose {
        None, Count, ToScalar, ToDynamic, ToTreeList, ToSingle, ToList, ToTreeNodePath;
    }

    QueryPurpose getPurpose();

    List<SqlSelectItemExpression> getSelects();

    int getSkipSize();

    int getTakeSize();

}
