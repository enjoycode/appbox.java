package appbox.store.query;

import appbox.expressions.Expression;
import appbox.store.expressions.SqlSelectItem;

import java.util.List;

public final class SqlFromQuery extends SqlQueryBase implements ISqlSelectQuery {
    @Override
    public QueryPurpose getPurpose() {
        return null;
    }

    @Override
    public List<SqlSelectItem> getSelects() {
        return null;
    }

    @Override
    public Expression getFilter() {
        return null;
    }

    @Override
    public int getSkipSize() {
        return 0;
    }

    @Override
    public int getTakeSize() {
        return 0;
    }

    @Override
    public List<SqlOrderBy> getOrderBy() {
        throw new RuntimeException("未实现");
    }

    @Override
    public List<SqlSelectItem> getGroupBy() {
        throw new RuntimeException("未实现");
    }

    @Override
    public Expression getHavingFilter() {
        throw new RuntimeException("未实现");
    }
}
