package appbox.store.query;

import appbox.expressions.EntityPathExpression;
import appbox.expressions.EntityExpression;
import appbox.expressions.Expression;
import appbox.model.EntityModel;
import appbox.runtime.RuntimeContext;
import appbox.store.DbTransaction;
import appbox.store.SqlStore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/** 用于更新满足指定条件的记录，支持同时返回指定字段值 */
public final class SqlUpdateCommand extends SqlQueryBase implements ISqlQuery {

    private      Expression             _filter;
    public final EntityExpression       t;
    /** 更新表达式 TODO:考虑使用BlockExpression支持多个t=>{t.V1=t.V1+1; t.V2=t.V2+2} */
    public final List<Expression>       updateItems = new ArrayList<>();
    private      EntityPathExpression[] _outputItems;
    private      Consumer<SqlRowReader> _readOutputs; //用于读取返回的输出

    public SqlUpdateCommand(long entityModelId) {
        t = new EntityExpression(entityModelId, this);
    }

    public EntityPathExpression m(String name) {
        return t.m(name);
    }

    @Override
    public Expression getFilter() {
        return _filter;
    }

    public boolean hasOutputs() {
        return _outputItems != null && _outputItems.length > 0;
    }

    public EntityPathExpression[] outputItems() {
        return _outputItems;
    }

    public SqlUpdateCommand update(Function<SqlUpdateCommand, Expression> assignment) {
        return update(assignment.apply(this));
    }

    public SqlUpdateCommand update(Expression assignment) {
        //TODO:验证
        updateItems.add(assignment);
        return this;
    }

    public SqlUpdateCommand where(Function<SqlUpdateCommand, Expression> condition) {
        _filter = condition.apply(this);
        return this;
    }

    public SqlUpdateCommand where(Expression filter) {
        _filter = filter;
        return this;
    }

    public <R> UpdateOutputs<R> output(Function<SqlRowReader, R> selector,
                                       Function<SqlUpdateCommand, EntityPathExpression[]> selects) {
        return output(selector, selects.apply(this));
    }


    public <R> UpdateOutputs<R> output(Function<SqlRowReader, R> selector,
                                       EntityPathExpression... selects) {
        //TODO:验证
        _outputItems = selects;
        var res = new UpdateOutputs<>(selector);
        _readOutputs = res::onResult;
        return res;
    }

    public EntityPathExpression[] select(EntityPathExpression... items) {
        for (var item : items) {
            if (item.owner != t) //only for t.XXX
                throw new RuntimeException("EntityRef path not allowed");
        }
        return items;
    }

    public void readOutputs(SqlRowReader reader) {
        _readOutputs.accept(reader);
    }

    public CompletableFuture<Long> execAsync() {
        return execAsync(null);
    }

    public CompletableFuture<Long> execAsync(DbTransaction txn) {
        EntityModel model    = RuntimeContext.current().getModel(t.modelId);
        var         sqlStore = SqlStore.get(model.sqlStoreOptions().storeModelId());
        return sqlStore.execUpdateAsync(this, txn);
    }

    public static class UpdateOutputs<T> {
        private final Function<SqlRowReader, T> selector;
        private final List<T>                   values = new ArrayList<>();

        public T get(int index) {
            return values.get(index);
        }

        public int size() {
            return values.size();
        }

        public UpdateOutputs(Function<SqlRowReader, T> selector) {
            this.selector = selector;
        }

        protected void onResult(SqlRowReader reader) {
            values.add(selector.apply(reader));
        }

    }

}
