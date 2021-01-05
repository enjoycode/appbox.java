package appbox.store;

import appbox.expressions.*;
import appbox.model.EntityModel;
import appbox.model.entity.DataFieldModel;
import appbox.runtime.RuntimeContext;
import appbox.store.expressions.SqlSelectItemExpression;
import appbox.store.query.*;

import java.util.Collection;
import java.util.List;

final class PgSqlQueryBuilder {

    public static DbCommand build(ISqlSelectQuery query) {
        var cmd = new DbCommand();
        var ctx = new QueryBuildContext(cmd, query);

        if (query.getPurpose() == ISqlSelectQuery.QueryPurpose.ToTreeList) {
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
            var selects = query.getSelects();
            if (selects == null || selects.size() == 0) {
                ctx.append(((SqlQueryBase) query).aliasName);
                ctx.append(".*");
            } else {
                boolean needSep = false;
                for (var s : selects) {
                    if (needSep) ctx.append(",");
                    else needSep = true;
                    buildSelectItem(s, ctx);
                }
            }
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
            ctx.append('"');
            ctx.append(model.getSqlTableName(false, null));
            ctx.append("\" ");
            ctx.append(q.aliasName);
        }

        //构建Where
        ctx.setBuildStep(QueryBuildContext.QueryBuildStep.BuildWhere);
        if (ctx.currentQuery.getFilter() != null) {
            ctx.append(" Where ");
            buildExpression(ctx.currentQuery.getFilter(), ctx);
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
        ctx.setBuildStep(QueryBuildContext.QueryBuildStep.BuildJoin);
        SqlQueryBase sq = (SqlQueryBase) ctx.currentQuery;
        //先处理每个手工的联接及每个手工联接相应的自动联接
        if (sq.hasJoins()) {
            buildJoins(sq.getJoins(), ctx);
        }
        ctx.buildQueryAutoJoins(sq, '\"'); //再处理自动联接

        //处理Skip and Take
        if (query.getPurpose() != ISqlSelectQuery.QueryPurpose.Count) {
            ctx.setBuildStep(QueryBuildContext.QueryBuildStep.BuildSkipAndTake);
            if (query.getSkipSize() > 0) {
                ctx.append(" Offset ");
                ctx.append(query.getSkipSize());
                ctx.append(' ');
            }

            if (query.getPurpose() == ISqlSelectQuery.QueryPurpose.ToSingle) {
                ctx.append(" Limit 1 ");
            } else if (query.getTakeSize() > 0) {
                ctx.append(" Limit ");
                ctx.append(query.getTakeSize());
                ctx.append(' ');
            }
        }

        ////构建分组、Having及排序
        //BuildGroupBy(query, ctx);

        //结束上下文
        ctx.endBuildQuery(query, false);
    }

    private static void buildSelectItem(SqlSelectItemExpression item, QueryBuildContext ctx) {
        //判断item.Expression是否是子Select项,是则表示外部查询（FromQuery）引用的Select项
        if (item.expression.getType() == ExpressionType.SelectItemExpression) {
            var si = (SqlSelectItemExpression) item.expression;
            //判断当前查询是否等于Select项的所有者，否则表示Select项的所有者的外部查询引用该Select项
            var ownerAliasName = ctx.currentQuery == item.owner ?
                    ctx.getQueryAliasName(si.owner) : ctx.getQueryAliasName(item.owner);
            ctx.append(ownerAliasName);
            ctx.append(".\"");
            ctx.append(si.aliasName);
            ctx.append('"');

            //处理选择项别名
            if (ctx.getBuildStep() == QueryBuildContext.QueryBuildStep.BuildSelect /* && !ctx.isBuildCteSelect*/) {
                if (!item.aliasName.equals(si.aliasName)) {
                    ctx.append(" \"");
                    ctx.append(item.aliasName);
                    ctx.append('"');
                }
            }
        } else { //----上面为FromQuery的Select项，下面为Query或SubQuery的Select项----
            //判断当前查询是否等于Select项的所有者，否则表示Select项的所有者的外部查询引用该Select项
            if (ctx.currentQuery == item.owner) {
                buildExpression(item.expression, ctx);
            } else {
                ctx.append(ctx.getQueryAliasName(item.owner));
                ctx.append(".\"");
                ctx.append(item.aliasName);
                ctx.append('"');
            }

            //处理选择项别名
            if (ctx.getBuildStep() == QueryBuildContext.QueryBuildStep.BuildSelect /* && !ctx.isBuildCteSelect*/) {
                boolean needAlias = true;
                if (item.expression instanceof EntityPathExpression) {
                    needAlias = !((EntityPathExpression) item.expression).name.equals(item.aliasName);
                }
                if (needAlias) {
                    ctx.append(" \"");
                    ctx.append(item.aliasName);
                    ctx.append('"');
                }
            }
        }
    }

    /** 处理手工联接及其对应的自动联接 */
    protected static void buildJoins(List<SqlJoin> joins, QueryBuildContext ctx) {
        for (var item : joins) {
            //先处理当前的联接
            ctx.append(getJoinString(item.joinType));
            if (item.right instanceof SqlQueryJoin) {
                var         j      = (SqlQueryJoin) item.right;
                EntityModel jmodel = RuntimeContext.current().getModel(j.t.modelId);
                ctx.append('\"');
                ctx.append(jmodel.getSqlTableName(false, null));
                ctx.append("\" ");
                ctx.append(j.aliasName);
                ctx.append(" ON ");
                buildExpression(item.onCondition, ctx);

                //再处理手工联接的自动联接
                ctx.buildQueryAutoJoins(j, '\"');
            } else { //否则表示联接对象是SubQuery，注意：子查询没有自动联接
                var sq = (SqlSubQuery) item.right;
                ctx.append('(');
                buildNormalQuery(sq.target, ctx);
                ctx.append(") AS ");
                ctx.append(((SqlQueryBase) sq.target).aliasName);
                ctx.append(" ON ");
                buildExpression(item.onCondition, ctx);
            }

            //最后递归当前联接的右部是否还有手工的联接项
            if (item.right.hasJoins())
                buildJoins(item.right.getJoins(), ctx);
        }
    }

    private static String getJoinString(SqlJoin.JoinType type) {
        switch (type) {
            case Inner:
                return " Join ";
            case Left:
                return " Left Join ";
            case Right:
                return " Right Join ";
            case Full:
                return " Full Join ";
            default:
                throw new RuntimeException("Unknown join type");
        }
    }

    //region ====Build Expression TODO:以下考虑移至QueryBuildContext内====
    protected static void buildExpression(Expression exp, QueryBuildContext ctx) {
        switch (exp.getType()) {
            case PrimitiveExpression:
                buildPrimitiveExpression((PrimitiveExpression) exp, ctx); break;
            case EntityExpression:
                buildEntityExpression((EntityExpression) exp, ctx); break;
            case FieldExpression:
                buildFieldExpression((EntityFieldExpression) exp, ctx); break;
            case BinaryExpression:
                buildBinaryExpression((BinaryExpression) exp, ctx); break;
            case DbFuncExpression:
                buildDbFuncExpression((DbFunc) exp, ctx); break;
            case SubQueryExpression:
                buildSubQuery((SqlSubQuery) exp, ctx); break;
            default:
                throw new RuntimeException("未实现: " + exp.getType().name());
        }
    }

    private static void buildSubQuery(SqlSubQuery exp, QueryBuildContext ctx) {
        ctx.append('(');
        buildNormalQuery(exp.target, ctx);
        ctx.append(')');
    }

    private static void buildPrimitiveExpression(PrimitiveExpression exp, QueryBuildContext ctx) {
        if (exp.value == null) {
            ctx.append("NULL");
            return;
        }

        if (exp.value instanceof Collection<?>) { //用于处理In及NotIn的参数
            ctx.append('(');
            boolean needSep    = false;
            var     collection = (Collection<?>) exp.value;
            for (var item : collection) {
                if (needSep)
                    ctx.append(',');
                else
                    needSep = true;
                ctx.addParameter(item);
                ctx.append('?');
            }
            ctx.append(')');
        } else {
            ctx.addParameter(exp.value);
            ctx.append('?');
        }
    }

    private static void buildEntityExpression(EntityExpression exp, QueryBuildContext ctx) {
        //判断是否已处理过
        if (exp.getAliasName() != null)
            return;

        //判断是否已到达根
        if (exp.owner == null) {
            //判断exp.User是否为Null，因为可能是附加的QuerySelectItem
            if (exp.getUser() == null) {
                var q = (SqlQueryBase) ctx.currentQuery;
                exp.setUser(q);
            }
            exp.setAliasName(((SqlQueryBase) exp.getUser()).aliasName);
        } else { //否则表示自动联接
            //先处理owner
            buildEntityExpression(exp.owner, ctx);
            //再获取自动联接的别名
            exp.setAliasName(ctx.getEntityRefAliasName(exp, (SqlQueryBase) exp.getUser()));
        }
    }

    private static void buildFieldExpression(EntityFieldExpression exp, QueryBuildContext ctx) {
        //判断上下文是否在处理Update的Set
        if (ctx.getBuildStep() == QueryBuildContext.QueryBuildStep.BuildUpdateSet) {
            ctx.append('"');
            ctx.append(exp.name);
            ctx.append('"');
        } else if (ctx.getBuildStep() == QueryBuildContext.QueryBuildStep.BuildUpsertSet) {
            EntityModel model = RuntimeContext.current().getModel(exp.owner.modelId);
            ctx.append('"');
            ctx.append(model.name());
            ctx.append("\".\"");
            ctx.append(exp.name);
            ctx.append('"');
        } else {
            buildEntityExpression(exp.owner, ctx);
            ctx.append(exp.owner.getAliasName());
            ctx.append(".\"");
            ctx.append(exp.name);
            ctx.append('"');
        }
    }

    private static void buildBinaryExpression(BinaryExpression exp, QueryBuildContext ctx) {
        //左表达式
        buildExpression(exp.leftOperand, ctx);

        //判断条件表达式中的null
        if (ctx.getBuildStep() == QueryBuildContext.QueryBuildStep.BuildWhere
                && exp.rightOperand.getType() == ExpressionType.PrimitiveExpression
                && ((PrimitiveExpression) exp.rightOperand).value == null) {
            if (exp.binaryType == BinaryExpression.BinaryOperatorType.Equal)
                ctx.append(" ISNULL");
            else if (exp.binaryType == BinaryExpression.BinaryOperatorType.NotEqual)
                ctx.append(" NOTNULL");
            else
                throw new UnsupportedOperationException();
        } else {
            //操作符
            buildBinaryOperator(exp, ctx);
            //右表达式, 暂在这里特殊处理Like等通配符
            if (exp.binaryType == BinaryExpression.BinaryOperatorType.Like)
                ctx.append('%');
            buildExpression(exp.rightOperand, ctx);
            if (exp.binaryType == BinaryExpression.BinaryOperatorType.Like)
                ctx.append('%');
        }
    }

    private static void buildBinaryOperator(BinaryExpression exp, QueryBuildContext ctx) {
        switch (exp.binaryType) {
            case AndAlso:
                ctx.append(" And "); break;
            case OrElse:
                ctx.append(" Or "); break;
            case BitwiseAnd:
                ctx.append(" & "); break;
            case BitwiseOr:
                ctx.append(" | "); break;
            case BitwiseXor:
                ctx.append(" ^ "); break;
            case Divide:
                ctx.append(" / "); break;
            case Assign:
            case Equal:
                ctx.append("="); break;
            case Greater:
                ctx.append(">"); break;
            case GreaterOrEqual:
                ctx.append(">="); break;
            case In:
                ctx.append(" In "); break;
            case NotIn:
                ctx.append(" Not In "); break;
            case Is:
                ctx.append(" Is "); break;
            case IsNot:
                ctx.append(" Is Not "); break;
            case Less:
                ctx.append("<"); break;
            case LessOrEqual:
                ctx.append("<="); break;
            case Like:
                ctx.append(" Like "); break;
            case Minus:
                ctx.append("-"); break;
            case Multiply:
                ctx.append("*"); break;
            case NotEqual:
                ctx.append(" <> "); break;
            case Plus:
                ctx.append(checkNeedConvertStringAddOperator(exp) ? " || " : "+"); break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    private static void buildDbFuncExpression(DbFunc exp, QueryBuildContext ctx) {
        ctx.append(exp.name);
        ctx.append('(');
        if (exp.arguments != null && exp.arguments.length > 0) {
            for (int i = 0; i < exp.arguments.length; i++) {
                if (i != 0)
                    ctx.append(',');
                buildExpression(exp.arguments[i], ctx);
            }
        }
        ctx.append(')');
    }

    /**
     * 用于字符串+连接时转换为||操作符
     * @return true需要转换
     */
    private static boolean checkNeedConvertStringAddOperator(Expression exp) {
        switch (exp.getType()) {
            case BinaryExpression:
                var be = (BinaryExpression) exp;
                return checkNeedConvertStringAddOperator(be.leftOperand)
                        || checkNeedConvertStringAddOperator(be.rightOperand);
            case FieldExpression:
                var fe = (EntityFieldExpression) exp;
                EntityModel model = RuntimeContext.current().getModel(fe.owner.modelId);
                var fieldModel = (DataFieldModel) model.tryGetMember(fe.name);
                return fieldModel.dataType() == DataFieldModel.DataFieldType.String;
            case PrimitiveExpression:
                return ((PrimitiveExpression) exp).value instanceof String;
            default:
                throw new UnsupportedOperationException();
        }
    }
    //endregion

}
