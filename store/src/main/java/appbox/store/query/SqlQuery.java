package appbox.store.query;

import appbox.data.SqlEntity;
import appbox.data.SqlEntityKVO;
import appbox.entities.EntityMemberValueGetter;
import appbox.entities.EntityMemberValueSetter;
import appbox.expressions.*;
import appbox.logging.Log;
import appbox.model.EntityModel;
import appbox.model.entity.EntityMemberModel;
import appbox.model.entity.EntityRefModel;
import appbox.model.entity.EntitySetModel;
import appbox.runtime.RuntimeContext;
import appbox.serialization.IEntityMemberWriter;
import appbox.store.SqlStore;
import appbox.store.expressions.SqlSelectItem;
import com.github.jasync.sql.db.RowData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class SqlQuery<T extends SqlEntity> extends SqlQueryBase implements ISqlSelectQuery {

    public final  EntityExpression    t;
    private final Class<T>            _clazz;
    private       QueryPurpose        _purpose;
    private       Expression          _filter;
    private       List<SqlSelectItem> _selects;
    private       int                 _skip = 0;
    private       int                 _take = 0;
    //order by
    private       List<SqlOrderBy>    _orderBy;
    //group by and having
    private       List<SqlSelectItem> _groupBy;
    private       Expression          _havingFilter;
    //用于EagerLoad导航属性
    private       SqlIncluder         _rootIncluder;
    //cache for tree query
    private       EntityRefModel      _treeParentMember;

    public SqlQuery(long modelId, Class<T> clazz) {
        t      = new EntityExpression(modelId, this);
        _clazz = clazz;
    }

    //region ====Properties====
    public EntityPathExpression m(String name) {
        return t.m(name);
    }

    @Override
    public QueryPurpose getPurpose() { return _purpose; }

    @Override
    public Expression getFilter() { return _filter;}

    @Override
    public List<SqlSelectItem> getSelects() {
        return _selects;
    }

    @Override
    public List<SqlSelectItem> getGroupBy() { return _groupBy; }

    @Override
    public Expression getHavingFilter() { return _havingFilter; }

    @Override
    public List<SqlOrderBy> getOrderBy() {
        return _orderBy;
    }

    public EntityRefModel getTreeParentMember() {
        return _treeParentMember;
    }
    //endregion

    //region ====Skip & Take====
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
    //endregion

    //region ====Include Methods====
    public SqlIncluder include(Function<EntityExpression, EntityPathExpression> select) {
        if (_rootIncluder == null)
            _rootIncluder = new SqlIncluder(t);
        return _rootIncluder.thenInclude(select);
    }
    //endregion

    //region ====Join====
    public ISqlQueryJoin leftJoin(ISqlQueryJoin target, BiFunction<SqlQuery<T>, ISqlQueryJoin, Expression> onCondition) {
        return join(SqlJoin.JoinType.Left, target, onCondition.apply(this, target));
    }

    public ISqlQueryJoin rightJoin(ISqlQueryJoin target, BiFunction<SqlQuery<T>, ISqlQueryJoin, Expression> onCondition) {
        return join(SqlJoin.JoinType.Right, target, onCondition.apply(this, target));
    }

    public ISqlQueryJoin innerJoin(ISqlQueryJoin target, BiFunction<SqlQuery<T>, ISqlQueryJoin, Expression> onCondition) {
        return join(SqlJoin.JoinType.Inner, target, onCondition.apply(this, target));
    }

    public ISqlQueryJoin fullJoin(ISqlQueryJoin target, BiFunction<SqlQuery<T>, ISqlQueryJoin, Expression> onCondition) {
        return join(SqlJoin.JoinType.Full, target, onCondition.apply(this, target));
    }
    //endregion

    //region ====Where Methods====
    public SqlQuery<T> where(Function<SqlQuery<T>, Expression> condition) {
        _filter = condition.apply(this);
        return this;
    }

    public SqlQuery<T> where(ISqlQueryJoin join, BiFunction<SqlQuery<T>, ISqlQueryJoin, Expression> condition) {
        _filter = condition.apply(this, join);
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

    //region ====OderBy Methods====
    public SqlQuery<T> orderBy(Function<SqlQuery<T>, Expression> select) {
        var item = new SqlOrderBy(select.apply(this));
        if (_orderBy == null)
            _orderBy = new ArrayList<>();
        _orderBy.add(item);
        return this;
    }

    public SqlQuery<T> orderByDesc(Function<SqlQuery<T>, Expression> select) {
        var item = new SqlOrderBy(select.apply(this), true);
        if (_orderBy == null)
            _orderBy = new ArrayList<>();
        _orderBy.add(item);
        return this;
    }
    //endregion

    //region ====Select Methods====
    protected static void addAllSelects(SqlQuery<?> query, EntityModel model, EntityExpression t, String fullPath) {
        //TODO:考虑特殊SqlSelectItemExpression with *，但只能在fullpath==null时使用
        for (var member : model.getMembers()) {
            if (member.type() == EntityMemberModel.EntityMemberType.DataField) {
                String alias = fullPath == null ? member.name() :
                        String.format("%s.%s", fullPath, member.name());
                var si = new SqlSelectItem(t.m(member.name()), alias);
                query.addSelect(si);
            }
        }
    }

    protected void addSelect(SqlSelectItem item) {
        if (_selects == null)
            _selects = new ArrayList<>();

        item.owner = this;
        _selects.add(item);
    }

    public Expression[] select(Expression... items) {
        return items;
    }

    public CompletableFuture<List<T>> toListAsync() {
        _purpose = QueryPurpose.ToList;
        EntityModel model = RuntimeContext.current().getModel(t.modelId);

        //添加选择项,暂默认*
        if (_rootIncluder != null) {
            addAllSelects(this, model, t, null);
            _rootIncluder.addSelects(this, model, null);
        }

        var db = SqlStore.get(model.sqlStoreOptions().storeModelId());
        return db.runQuery(this).thenApply(res -> {
            //Log.debug("共读取: " + res.getRows().size());
            var rows      = res.getRows();
            var rowReader = new SqlRowReader(rows.columnNames());
            var list      = new ArrayList<T>(rows.size());
            var creator   = getEntityCreator(model);
            for (var row : rows) {
                rowReader.rowData = row;
                var obj = creator.get();
                fillEntity(obj, model, rowReader, 0);
                list.add(obj);
            }

            return list;
        });
    }

    @SuppressWarnings("unchecked")
    private Supplier<T> getEntityCreator(EntityModel model) {
        if (_clazz == SqlEntityKVO.class) {
            return () -> (T) new SqlEntityKVO(model);
        }

        try {
            final var ctor = _clazz.getDeclaredConstructor();
            return () -> {
                try {
                    return ctor.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public <R> CompletableFuture<List<R>> toListAsync(Function<SqlRowReader, ? extends R> mapper,
                                                      Function<SqlQuery<T>, Expression[]> selects) {
        return toListAsync(mapper, selects.apply(this));
    }

    public <R> CompletableFuture<List<R>> toListAsync(Function<SqlRowReader, ? extends R> mapper,
                                                      Expression... selects) {
        if (selects == null || selects.length == 0)
            throw new IllegalArgumentException("must select some one");

        _purpose = QueryPurpose.ToDynamic;

        //Add selects
        if (_selects != null)
            _selects.clear();
        for (var select : selects) {
            addSelect(new SqlSelectItem(select));
        }

        EntityModel model = RuntimeContext.current().getModel(t.modelId);
        var         db    = SqlStore.get(model.sqlStoreOptions().storeModelId());
        return db.runQuery(this).thenApply(res -> {
            //Log.debug("共读取: " + res.getRows().size());
            var rows        = res.getRows();
            var rowReader   = new SqlRowReader(rows.columnNames());
            var list        = new ArrayList<R>(rows.size());
            int extendsFlag = -1; //-1=未知状态,0=非扩展, >0扩展数量
            for (RowData row : rows) {
                rowReader.rowData = row;
                R obj = mapper.apply(rowReader);
                if (extendsFlag == -1) {
                    extendsFlag = _clazz.isInstance(obj) ? obj.getClass().getDeclaredFields().length : 0;
                }
                if (extendsFlag > 0) { //如果是扩展类，则填充本身成员
                    fillEntity((SqlEntity) obj, model, rowReader, extendsFlag);
                }
                list.add(obj);
            }

            return list;
        });
    }

    /**
     * 返回树状结构的实体集合
     * @param childrenMember eg: q ->q.m("Children")
     */
    public CompletableFuture<List<T>> toTreeAsync(Function<SqlQuery<T>, Expression> childrenMember) {
        var         children      = (EntitySetExpression) childrenMember.apply(this);
        EntityModel model         = RuntimeContext.current().getModel(t.modelId);
        var         childrenModel = (EntitySetModel) model.getMember(children.name);
        _treeParentMember = (EntityRefModel) model.getMember(childrenModel.refMemberId());

        addAllSelects(this, model, t, null);

        //TODO:EntitySet自动排序

        //如果没有设置任何条件，则设置默认条件为查询根级开始
        if (_filter == null) {
            for (var fk : _treeParentMember.getFKMemberIds()) {
                var con = t.m(model.getMember(fk).name()).eq(null);
                _filter = _filter == null ? con : _filter.and(con);
            }
        }

        _purpose = QueryPurpose.ToTreeList;
        var db = SqlStore.get(model.sqlStoreOptions().storeModelId());
        return db.runQuery(this).thenApply(res -> {
            var rows      = res.getRows();
            var rowReader = new SqlRowReader(rows.columnNames());
            var list      = new ArrayList<T>(rows.size());
            var creator   = getEntityCreator(model);
            var dic       = new HashMap<Object, T>(rows.size());
            var getter    = new EntityMemberValueGetter();
            for (var row : rows) {
                rowReader.rowData = row;
                var obj = creator.get();
                fillEntity(obj, model, rowReader, 1);
                var treeLevel = row.getInt(row.size() - 1);
                if (treeLevel == 0) {
                    list.add(obj);
                } else {
                    var parent = dic.get(getFKS(_treeParentMember, obj, getter));
                    //TODO:*** set child.Parent = parent
                    @SuppressWarnings("unchecked")
                    var childrenList = (List<T>) parent.getNaviPropForFetch(childrenModel.name());
                    childrenList.add(obj);
                }
                dic.put(getPKS(model, obj, getter), obj);
            }

            dic.clear();
            return list;
        });
    }
    //endregion

    //region ====SubQuery & FromQuery====
    public SqlSubQuery toSubQuery(Function<SqlQuery<T>, Expression> selects) {
        var exp = selects.apply(this);
        addSelect(new SqlSelectItem(exp));
        return new SqlSubQuery(this);
    }
    //endregion

    //region ====GroupBy Methods====
    public SqlQuery<T> groupBy(Function<SqlQuery<T>, Expression> select) {
        var key = new SqlSelectItem(select.apply(this));
        key.owner = this;
        if (_groupBy == null)
            _groupBy = new ArrayList<>();
        _groupBy.add(key);
        return this;
    }

    public SqlQuery<T> having(Function<SqlQuery<T>, Expression> condition) {
        _havingFilter = condition.apply(this);
        return this;
    }
    //endregion

    //region ====Fetch Entity Methods====
    private static void fillEntity(SqlEntity entity, EntityModel model, SqlRowReader row, int extendsFlag) {
        //填充实体成员
        for (int i = 0; i < row.columns.size() - extendsFlag; i++) {
            fillMember(model, entity, row.columns.get(i), row, i);
        }
        //需要改变实体持久化状态(不同于C#实现)
        entity.fetchDone();
    }

    private static void fillMember(EntityModel model, SqlEntity entity,
                                   String path, SqlRowReader row, int clIndex) {
        if (row.isNull(clIndex))
            return;

        var indexOfDot = path.indexOf('.');
        if (indexOfDot < 0) {
            var member = model.tryGetMember(path); //TODO:考虑生成运行时简化代码，直接映射Name->Id
            if (member == null) { //不存在通过反射处理, 如扩展的引用字段
                Log.warn(String.format("未找到实体成员%s.%s", model.name(), path));
            } else {
                entity.readMember(member.memberId(), row, clIndex);
            }
        } else {
            var name = path.substring(0, indexOfDot);
            var entityRefModel = (EntityRefModel)model.getMember(name);
            if (entityRefModel.isAggregationRef())
                throw new RuntimeException("未实现");
            var entityRef = (SqlEntity) entity.getNaviPropForFetch(name);
            fillMember(entityRef.model(), entityRef, path.substring(indexOfDot + 1), row, clIndex);
            entityRef.fetchDone(); //TODO:暂每填完一个成员调用一次
        }
    }

    /** 单主键直接返回主键值，多主键List（不能使用Array） */
    private static Object getPKS(EntityModel model, SqlEntity entity, EntityMemberValueGetter getter) {
        var options = model.sqlStoreOptions();
        if (options.primaryKeys().length == 1) {
            entity.writeMember(options.primaryKeys()[0].memberId, getter, IEntityMemberWriter.SF_NONE);
            return getter.value;
        }

        var list = new ArrayList<Object>(options.primaryKeys().length);
        for (var pk : options.primaryKeys()) {
            entity.writeMember(pk.memberId, getter, IEntityMemberWriter.SF_NONE);
            list.add(getter.value);
        }
        return list;
    }

    private static Object getFKS(EntityRefModel entityRefModel, SqlEntity entity, EntityMemberValueGetter getter) {
        if (entityRefModel.getFKMemberIds().length == 1) {
            entity.writeMember(entityRefModel.getFKMemberIds()[0], getter, IEntityMemberWriter.SF_NONE);
            return getter.value;
        }

        var list = new ArrayList<Object>(entityRefModel.getFKMemberIds().length);
        for (var fk : entityRefModel.getFKMemberIds()) {
            entity.writeMember(fk, getter, IEntityMemberWriter.SF_NONE);
            list.add(getter.value);
        }
        return list;
    }
    //endregion

}
