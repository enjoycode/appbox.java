package appbox.store.query;

import appbox.data.SqlEntity;
import appbox.expressions.EntityBaseExpression;
import appbox.expressions.EntityExpression;
import appbox.expressions.Expression;
import appbox.logging.Log;
import appbox.model.EntityModel;
import appbox.runtime.RuntimeContext;
import appbox.store.SqlStore;
import appbox.store.expressions.SqlSelectItemExpression;
import com.github.jasync.sql.db.RowData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class SqlQuery<T extends SqlEntity> extends SqlQueryBase implements ISqlSelectQuery {

    public final  EntityExpression                         t;
    private final Class<T>                                 _clazz;
    private       QueryPurpose                             _purpose;
    private       Expression                               _filter;
    private       HashMap<String, SqlSelectItemExpression> _selects;

    public SqlQuery(long modelId, Class<T> clazz) {
        t      = new EntityExpression(modelId, this);
        _clazz = clazz;
    }

    @Override
    public QueryPurpose getPurpose() { return _purpose; }

    @Override
    public Expression getFilter() { return _filter;}

    //region ====Select Methods====

    /** 简化选择 */
    public void select(EntityBaseExpression member) {
        addSelect(new SqlSelectItemExpression(member));
    }

    public void addSelect(SqlSelectItemExpression item) {
        if (_selects == null)
            _selects = new HashMap<>();

        item.owner = this;
        _selects.put(item.aliasName, item);
    }

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
            var rows      = res.getRows();
            var rowReader = new SqlRowReader(rows.columnNames());
            var list      = new ArrayList<T>(rows.size());
            try {
                for (RowData row : rows) {
                    rowReader.rowData = row;
                    T obj = fillEntity(_clazz, model, rowReader);
                    list.add(obj);
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            return list;
        });
    }

    public <R> CompletableFuture<List<R>> toListAsync(Function<SqlRowReader, ? extends R> mapper,
                                                      SqlSelectItemExpression... selects) {
        if (selects == null || selects.length == 0)
            throw new IllegalArgumentException("must select some one");

        _purpose = QueryPurpose.ToDynamic;

        //Add selects
        if (_selects != null)
            _selects.clear();
        for (var select : selects) {
            addSelect(select);
        }

        EntityModel model = RuntimeContext.current().getModel(t.modelId);
        var         db    = SqlStore.get(model.sqlStoreOptions().getStoreModelId());
        return db.runQuery(this).thenApply(res -> {
            Log.debug("共读取: " + res.getRows().size());
            var rows      = res.getRows();
            var rowReader = new SqlRowReader(rows.columnNames());
            var list      = new ArrayList<R>(rows.size());
            for (RowData row : rows) {
                rowReader.rowData = row;
                R obj = mapper.apply(rowReader);
                list.add(obj);
            }

            return list;
        });
    }
    //endregion

    //region ====static helpers====
    public static <E extends SqlEntity> E fillEntity(Class<E> clazz, EntityModel model, SqlRowReader row) throws Exception {
        //新建实体
        E obj = clazz.getDeclaredConstructor().newInstance();
        //填充实体成员
        for (int i = 0; i < row.columns.size(); i++) {
            fillMember(model, obj, row.columns.get(i), row, i);
        }

        //不需要obj.AcceptChanges()，新建时已处理持久状态
        return obj;
    }

    private static <E extends SqlEntity> void fillMember(
            EntityModel model, E target, String path, SqlRowReader row, int clIndex) throws Exception {
        if (row.isNull(clIndex))
            return;

        var indexOfDot = path.indexOf('.');
        if (indexOfDot < 0) {
            var member = model.tryGetMember(path);
            if (member == null) { //不存在通过反射处理, 如扩展的引用字段
                Log.warn(String.format("未找到实体成员%s.%s", model.name(), path));
            } else {
                target.readMember(member.memberId(), row, clIndex);
            }
        } else {
            throw new RuntimeException("未实现");
        }
    }
    //endregion

}
