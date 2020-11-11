package appbox.store.query;

import appbox.expressions.Expression;

public final class SqlFromQuery extends SqlQueryBase implements ISqlSelectQuery {
    @Override
    public QueryPurpose getPurpose() {
        return null;
    }

    @Override
    public Expression getFilter() {
        return null;
    }
}
