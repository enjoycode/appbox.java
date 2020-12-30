package appbox.store.query;

import appbox.expressions.EntityBaseExpression;
import appbox.expressions.EntityExpression;
import appbox.expressions.Expression;
import appbox.model.EntityModel;
import appbox.runtime.RuntimeContext;
import appbox.store.DbTransaction;
import appbox.store.SqlStore;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class SqlDeleteCommand extends SqlQueryBase implements ISqlQuery {

    private      Expression       _filter;
    public final EntityExpression t;

    public SqlDeleteCommand(long entityModelId) {
        t = new EntityExpression(entityModelId, this);
    }

    public EntityBaseExpression m(String name) {
        return t.m(name);
    }

    @Override
    public Expression getFilter() {
        return _filter;
    }

    public SqlDeleteCommand where(Function<SqlDeleteCommand, Expression> condition) {
        _filter = condition.apply(this);
        return this;
    }

    public SqlDeleteCommand where(Expression filter) {
        _filter = filter;
        return this;
    }

    public CompletableFuture<Long> execAsync() {
        return execAsync(null);
    }

    public CompletableFuture<Long> execAsync(DbTransaction txn) {
        EntityModel model    = RuntimeContext.current().getModel(t.modelId);
        var         sqlStore = SqlStore.get(model.sqlStoreOptions().storeModelId());
        return sqlStore.execDeleteAsync(this, txn);
    }

}
