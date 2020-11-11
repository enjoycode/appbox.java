package appbox.store;

import appbox.model.EntityModel;
import appbox.runtime.RuntimeContext;
import appbox.store.query.ISqlSelectQuery;
import appbox.store.query.SqlFromQuery;
import appbox.store.query.SqlQuery;

final class PgSqlQueryBuilder {

    public static DbCommand build(ISqlSelectQuery query) {
        var cmd = new DbCommand();
        var ctx = new QueryBuildContext(cmd, query);

        if(query.getPurpose() == ISqlSelectQuery.QueryPurpose.ToTreeList) {
            throw new RuntimeException("未实现");
        } else if (query.getPurpose() == ISqlSelectQuery.QueryPurpose.ToTreeNodePath) {
            throw new RuntimeException("未实现");
        } else {
            buildNormalQuery(query, ctx);
        }
        return cmd;
    }

    private static void buildNormalQuery(ISqlSelectQuery query, QueryBuildContext ctx) {
        ctx.beginBuildQuery(query);

        //构建select
        ctx.append("Select ");
        //if (query.getPurpose() == QueryPurpose.ToDataTable && query.Distinct)
        //    ctx.append("Distinct ");

        //构建Select Items
        ctx.setBuildStep(QueryBuildContext.QueryBuildStep.BuildSelect);
        if (query.getPurpose() == ISqlSelectQuery.QueryPurpose.Count) {
            ctx.append("Count(*)");
        } else {
            //TODO:fix
            ctx.append("*");
        }

        //构建From
        ctx.setBuildStep(QueryBuildContext.QueryBuildStep.BuildFrom);
        ctx.append(" From ");
        //判断From源
        if (query instanceof SqlFromQuery) {
            throw new RuntimeException("未实现");
        } else {
            var         q     = (SqlQuery<?>) ctx.currentQuery;
            EntityModel model = RuntimeContext.current().getModel(q.t.modelId);
            ctx.append(String.format("\"%s\" %s", model.getSqlTableName(false, null), q.aliasName));
        }

        //构建Where
        ctx.setBuildStep(QueryBuildContext.QueryBuildStep.BuildWhere);
        if (ctx.currentQuery.getFilter() != null) {
            throw new RuntimeException("未实现");
            //ctx.append(" Where ");
            //buildExpression(ctx.CurrentQuery.Filter, ctx);
        }

        //非分组的情况下构建Order By
        //if (query.getPurpose() != ISqlSelectQuery.QueryPurpose.Count) {
            //if (query.GroupByKeys == null && query.HasSortItems)
            //{
            //    ctx.CurrentQueryInfo.BuildStep = BuildQueryStep.BuildOrderBy;
            //    BuildOrderBy(query, ctx);
            //}
        //}

        //构建Join
        //ctx.CurrentQueryInfo.BuildStep = BuildQueryStep.BuildJoin;
        //SqlQueryBase q1 = (SqlQueryBase)ctx.CurrentQuery;
        //if (q1.HasJoins) //先处理每个手工的联接及每个手工联接相应的自动联接
        //{
        //    BuildJoins(q1.Joins, ctx);
        //}
        //ctx.BuildQueryAutoJoins(q1); //再处理自动联接
        //
        ////处理Skip and Take
        //if (query.Purpose != QueryPurpose.Count)
        //{
        //    ctx.CurrentQueryInfo.BuildStep = BuildQueryStep.BuildSkipAndTake;
        //    if (query.SkipSize > 0)
        //        ctx.AppendFormat(" Offset {0}", query.SkipSize);
        //    if (query.Purpose == QueryPurpose.ToSingleEntity)
        //        ctx.Append(" Limit 1 ");
        //    else if (query.TakeSize > 0)
        //        ctx.AppendFormat(" Limit {0} ", query.TakeSize);
        //}
        //
        ////构建分组、Having及排序
        //BuildGroupBy(query, ctx);

        //结束上下文
        ctx.endBuildQuery(query, false);
    }

}
