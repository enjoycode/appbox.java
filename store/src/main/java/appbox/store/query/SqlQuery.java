package appbox.store.query;

import appbox.data.SqlEntity;
import appbox.expressions.EntityExpression;
import appbox.expressions.Expression;
import appbox.logging.Log;
import appbox.model.EntityModel;
import appbox.runtime.RuntimeContext;
import appbox.store.SqlStore;
import com.github.jasync.sql.db.Connection;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SqlQuery<T extends SqlEntity> extends SqlQueryBase implements ISqlSelectQuery {

    public final EntityExpression t;
    private      QueryPurpose     _purpose;
    private      Expression       _filter;

    public SqlQuery(long modelId) {
        t = new EntityExpression(modelId, this);
    }

    @Override
    public QueryPurpose getPurpose() { return _purpose; }

    @Override
    public Expression getFilter() { return _filter;}

    public CompletableFuture<List<T>> toListAsync() {
        _purpose = QueryPurpose.ToList;
        EntityModel model = RuntimeContext.current().getModel(t.modelId);

        //TODO:添加选择项,暂默认*
        //AddAllSelects(this, model, T, null);
        //if (_rootIncluder != null)
        //    await _rootIncluder.AddSelects(this, model);

        var db = SqlStore.get(model.sqlStoreOptions().getStoreModelId());
        return db.runQuery(this).thenApply(res -> {
            Log.debug("共读取: " + res.getRows().size());
            return null; //TODO:
        });
    }


}
