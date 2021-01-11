package appbox.store;

import appbox.expressions.EntityExpression;
import appbox.model.EntityModel;
import appbox.model.entity.DataFieldModel;
import appbox.model.entity.EntityRefModel;
import appbox.runtime.RuntimeContext;
import appbox.store.query.ISqlQuery;
import appbox.store.query.SqlQueryBase;
import appbox.store.query.SqlQueryJoin;
import appbox.store.query.SqlSubQuery;

import java.util.HashMap;

final class QueryBuildContext {
    private final ISqlQuery rootQuery;     //根查询
    private final DbCommand command;

    public  ISqlQuery currentQuery;  //当前正在处理的查询
    private QueryInfo currentQueryInfo;

    private int _queryIndex;

    private final HashMap<ISqlQuery, QueryInfo>                            queries = new HashMap<>();
    private       HashMap<SqlQueryBase, HashMap<String, EntityExpression>> autoJoins;

    public QueryBuildContext(DbCommand cmd, ISqlQuery root) {
        command                              = cmd;
        rootQuery                            = root;
        ((SqlQueryBase) rootQuery).aliasName = "t";
        queries.put(rootQuery, new QueryInfo(rootQuery));
    }

    public void beginBuildQuery(ISqlQuery query) {
        QueryInfo qi = null;

        //尚未处理过，则新建相应的QueryInfo并加入字典表
        //注意：根查询在构造函数时已加入字典表
        qi = queries.get(query);
        if (qi == null)
            qi = addSubQuery(query);

        //设置上级的查询及相应的查询信息
        if (query != rootQuery) {
            qi.parentQuery = currentQuery;
            qi.parentInfo  = currentQueryInfo;
        }
        //设置当前的查询及相应的查询信息
        currentQuery     = query;
        currentQueryInfo = qi;

        //添加手工联接
        loopAddQueryJoins((SqlQueryBase) query);
    }

    public void endBuildQuery(ISqlQuery query, boolean cte) {
        //判断是否根查询
        if (currentQuery == rootQuery) {
            command.setCommandText(currentQueryInfo.getCommandText(cte));
        } else {
            currentQueryInfo.endBuildQuery();
            currentQuery     = currentQueryInfo.parentQuery;
            currentQueryInfo = currentQueryInfo.parentInfo;
        }
    }

    public void endBuildCurrentQuery() {
        currentQueryInfo.endBuildQuery();
    }

    public void setBuildStep(QueryBuildStep step) {
        currentQueryInfo.buildStep = step;
    }

    public QueryBuildStep getBuildStep() { return currentQueryInfo.buildStep; }

    public void append(char ch) {
        currentQueryInfo.getOut().append(ch);
    }

    public void append(int value) {
        currentQueryInfo.getOut().append(value);
    }

    public void append(String sql) {
        currentQueryInfo.getOut().append(sql);
    }

    public void addParameter(Object value) {
        //TODO:转换无符号类型为有符号类型
        command.addParameter(value);
    }

    /** 获取查询的别名,如果上下文中尚未存在查询，则自动设置别名并加入查询字典表 */
    public String getQueryAliasName(ISqlQuery query) {
        QueryInfo qi = queries.get(query);
        if (qi == null)
            qi = addSubQuery(query); //添加时会设置别名
        return ((SqlQueryBase) query).aliasName;
    }

    public String getEntityRefAliasName(EntityExpression exp, SqlQueryBase query) {
        var path = exp.toString();
        var ds   = autoJoins.get(query);
        var e    = ds.get(path);
        if (e == null) {
            ds.put(path, exp);
            _queryIndex += 1;
            exp.setAliasName("j" + _queryIndex);
            e = exp;
        }
        return e.getAliasName();
    }

    /** 添加指定的子查询至查询字典表 */
    private QueryInfo addSubQuery(ISqlQuery query) {
        //先判断是否已存在于手工Join里，如果不存在则需要设置别名
        var q = (SqlQueryBase) query;
        if (autoJoins == null || !autoJoins.containsKey(q)) {
            _queryIndex += 1;
            ((SqlQueryBase) query).aliasName = "t" + _queryIndex;
        }
        QueryInfo info = new QueryInfo(query, currentQueryInfo);
        queries.put(query, info);
        return info;
    }

    private void loopAddQueryJoins(SqlQueryBase query) {
        //判断是否已经生成别名
        if (query.aliasName == null) {
            _queryIndex += 1;
            query.aliasName = "t" + _queryIndex;
        }

        //将当前查询加入自动联接字典表
        if (autoJoins == null)
            autoJoins = new HashMap<>();
        autoJoins.put(query, new HashMap<>());

        if (query.hasJoins()) {
            for (var item : query.getJoins()) {
                if (item.right instanceof SqlQueryJoin) { //注意：子查询不具备自动联接
                    loopAddQueryJoins((SqlQueryBase) item.right);
                } else {
                    loopAddSubQueryJoins((SqlSubQuery) item.right);
                }
            }
        }
    }

    private void loopAddSubQueryJoins(SqlSubQuery query) {
        if (query.hasJoins()) {
            for (var item : query.getJoins()) {
                if (item.right instanceof SqlQueryJoin) { //注意：子查询不具备自动联接
                    loopAddQueryJoins((SqlQueryBase) item.right);
                } else {
                    loopAddSubQueryJoins((SqlSubQuery) item.right);
                }
            }
        }
    }

    /** 用于生成EntityRef的自动Join */
    public void buildQueryAutoJoins(SqlQueryBase target, char nameEscaper) {
        if (autoJoins == null)
            return;
        var ds = autoJoins.get(target);
        if (ds == null)
            return;

        for (var rq : ds.values()) {
            //Left Join "City" c ON c."Code" = t."CityCode"

            //eg: Customer.City的City
            EntityModel rqModel = RuntimeContext.current().getModel(rq.modelId);
            //eg: Customer.City的Customer
            EntityModel rqOwnerModel = RuntimeContext.current().getModel(rq.owner.modelId);
            append(" Left Join ");
            append(nameEscaper);
            append(rqModel.getSqlTableName(false, null));
            append(nameEscaper);
            append(' ');
            append(rq.getAliasName());
            append(" ON ");
            //Build ON Condition, other.pks == this.fks
            var rm = (EntityRefModel) rqOwnerModel.getMember(rq.name);
            for (int i = 0; i < rqModel.sqlStoreOptions().primaryKeys().length; i++) {
                if (i != 0)
                    append(" And ");
                var pk = (DataFieldModel) rqModel.getMember(rqModel.sqlStoreOptions().primaryKeys()[i].memberId);
                var fk = (DataFieldModel) rqOwnerModel.getMember(rm.getFKMemberIds()[i]);
                append(rq.getAliasName());
                append('.');
                append(nameEscaper);
                append(pk.sqlColName());
                append(nameEscaper);
                append('=');
                append(rq.owner.getAliasName());
                append('.');
                append(nameEscaper);
                append(fk.sqlColName());
                append(nameEscaper);
            }
        }
    }

    //region ====QueryInfo & QueryBuildStep====
    static final class QueryInfo {
        private final StringBuilder  sb1;
        private final StringBuilder  sb2;
        public final  ISqlQuery      owner;
        public        QueryBuildStep buildStep;
        public        ISqlQuery      parentQuery;
        public        QueryInfo      parentInfo;

        /** 构造根查询信息 */
        public QueryInfo(ISqlQuery owner) {
            this.owner = owner;
            sb1        = new StringBuilder(100);
            sb2        = new StringBuilder(100);
        }

        /** 构造子查询信息 */
        public QueryInfo(ISqlQuery owner, QueryInfo parentInfo) {
            this.owner = owner;
            if (parentInfo.buildStep == QueryBuildStep.BuildWhere)
                sb1 = parentInfo.sb2;
            else
                sb1 = parentInfo.sb1;
            sb2 = new StringBuilder(100);
        }

        public StringBuilder getOut() {
            if (buildStep == QueryBuildStep.BuildWhere
                    || buildStep == QueryBuildStep.BuildOrderBy
                    || buildStep == QueryBuildStep.BuildSkipAndTake
                    || buildStep == QueryBuildStep.BuildPageTail)
                return sb2;
            return sb1;
        }

        public void endBuildQuery() {
            sb1.append(sb2);
        }

        public String getCommandText(boolean cte) {
            if (!cte) {
                sb1.append(sb2);
            }
            return sb1.toString();
        }
    }

    public enum QueryBuildStep {
        BuildSelect, BuildFrom, BuildJoin, BuildWhere, BuildGroupBy, BuildOrderBy, BuildUpdateSet,
        BuildUpsertSet, BuildWithCTE, BuildPageTail, BuildPageOrderBy, BuildSkipAndTake, BuildHaving;
    }
    //endregion
}
