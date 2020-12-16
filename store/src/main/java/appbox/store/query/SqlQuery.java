package appbox.store.query;

import appbox.data.SqlEntity;
import appbox.data.SqlEntityKVO;
import appbox.expressions.BinaryExpression;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

public class SqlQuery<T extends SqlEntity> extends SqlQueryBase implements ISqlSelectQuery {

    public final  EntityExpression              t;
    private final Class<T>                      _clazz;
    private       QueryPurpose                  _purpose;
    private       Expression                    _filter;
    private       List<SqlSelectItemExpression> _selects;
    private       int                           _skip = 0;
    private       int                           _take = 0;

    public SqlQuery(long modelId, Class<T> clazz) {
        t      = new EntityExpression(modelId, this);
        _clazz = clazz;
    }

    public EntityBaseExpression m(String name) {
        return t.m(name);
    }

    @Override
    public QueryPurpose getPurpose() { return _purpose; }

    @Override
    public Expression getFilter() { return _filter;}

    @Override
    public List<SqlSelectItemExpression> getSelects() {
        return _selects;
    }

    @Override
    public int getSkipSize() {
        return _skip;
    }

    @Override
    public int getTakeSize() {
        return _take;
    }

    public SqlQuery<T> skip(int rows) {
        _skip = rows;
        return this;
    }

    public SqlQuery<T> take(int rows) {
        _take = rows;
        return this;
    }

    //region ====Where Methods====
    public SqlQuery<T> where(Function<SqlQuery<T>, Expression> condition) {
        _filter = condition.apply(this);
        return this;
    }

    public SqlQuery<T> where(Expression condition) {
        _filter = condition;
        return this;
    }

    public SqlQuery<T> andWhere(Expression condition) {
        if (_filter == null)
            _filter = condition;
        else
            _filter = new BinaryExpression(_filter, condition, BinaryExpression.BinaryOperatorType.AndAlso);
        return this;
    }

    public SqlQuery<T> orWhere(Expression condition) {
        if (_filter == null)
            _filter = condition;
        else
            _filter = new BinaryExpression(_filter, condition, BinaryExpression.BinaryOperatorType.OrElse);
        return this;
    }
    //endregion

    //region ====Select Methods====
    public void addSelect(SqlSelectItemExpression item) {
        if (_selects == null)
            _selects = new ArrayList<>();

        item.owner = this;
        _selects.add(item);
    }

    public CompletableFuture<List<T>> toListAsync() {
        _purpose = QueryPurpose.ToList;
        EntityModel model = RuntimeContext.current().getModel(t.modelId);

        //TODO:添加选择项,暂默认*
        //AddAllSelects(this, model, T, null);
        //if (_rootIncluder != null)
        //    await _rootIncluder.AddSelects(this, model);

        var db = SqlStore.get(model.sqlStoreOptions().storeModelId());
        return db.runQuery(this).thenApply(res -> {
            //Log.debug("共读取: " + res.getRows().size());
            var rows      = res.getRows();
            var rowReader = new SqlRowReader(rows.columnNames());
            var list      = new ArrayList<T>(rows.size());
            try {
                Supplier<T> creator;
                if (_clazz == SqlEntityKVO.class) {
                    creator = () -> (T) new SqlEntityKVO(model);
                } else {
                    final var ctor = _clazz.getDeclaredConstructor();
                    creator = () -> {
                        try {
                            return ctor.newInstance();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    };
                }

                for (RowData row : rows) {
                    rowReader.rowData = row;
                    var obj = creator.get();
                    fillEntity(obj, model, rowReader);
                    list.add(obj);
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            return list;
        });
    }

    public <R> CompletableFuture<List<R>> toListAsync(Function<SqlRowReader, ? extends R> mapper,
                                                      EntityBaseExpression... selects) {
        if (selects == null || selects.length == 0)
            throw new IllegalArgumentException("must select some one");

        _purpose = QueryPurpose.ToDynamic;

        //Add selects
        if (_selects != null)
            _selects.clear();
        for (var select : selects) {
            addSelect(new SqlSelectItemExpression(select));
        }

        EntityModel model = RuntimeContext.current().getModel(t.modelId);
        var         db    = SqlStore.get(model.sqlStoreOptions().storeModelId());
        return db.runQuery(this).thenApply(res -> {
            Log.debug("共读取: " + res.getRows().size());
            var rows        = res.getRows();
            var rowReader   = new SqlRowReader(rows.columnNames());
            var list        = new ArrayList<R>(rows.size());
            int extendsFlag = -1; //未知状态
            try {
                for (RowData row : rows) {
                    rowReader.rowData = row;
                    R obj = mapper.apply(rowReader);
                    if (extendsFlag == -1) {
                        extendsFlag = _clazz.isInstance(obj) ? 1 : 0;
                    }
                    if (extendsFlag == 1) { //如果是扩展类，则填充本身成员
                        fillEntity((SqlEntity) obj, model, rowReader);
                    }
                    list.add(obj);
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex); //never be here
            }

            return list;
        });
    }
    //endregion

    //region ====Fetch Entity Methods====
    private static void fillEntity(SqlEntity entity, EntityModel model, SqlRowReader row) {
        //填充实体成员
        for (int i = 0; i < row.columns.size(); i++) {
            fillMember(model, entity, row.columns.get(i), row, i);
        }
        //不需要obj.AcceptChanges()，新建时已处理持久状态
    }

    private static void fillMember(
            EntityModel model, SqlEntity entity, String path, SqlRowReader row, int clIndex) {
        if (row.isNull(clIndex))
            return;

        var indexOfDot = path.indexOf('.');
        if (indexOfDot < 0) {
            var member = model.tryGetMember(path);
            if (member == null) { //不存在通过反射处理, 如扩展的引用字段
                Log.warn(String.format("未找到实体成员%s.%s", model.name(), path));
            } else {
                entity.readMember(member.memberId(), row, clIndex);
            }
        } else {
            throw new RuntimeException("未实现");
        }
    }
    //endregion

}
