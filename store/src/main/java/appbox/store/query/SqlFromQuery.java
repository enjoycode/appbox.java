package appbox.store.query;

import appbox.expressions.Expression;
import appbox.store.expressions.SqlSelectItemExpression;

import java.util.Collection;

public final class SqlFromQuery extends SqlQueryBase implements ISqlSelectQuery {
    @Override
    public QueryPurpose getPurpose() {
        return null;
    }

    @Override
    public Collection<SqlSelectItemExpression> getSelects() {
        return null;
    }

    @Override
    public Expression getFilter() {
        return null;
    }
}
