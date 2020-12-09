package appbox.store;

import appbox.data.PersistentState;
import appbox.design.IDesignContext;
import appbox.model.EntityModel;
import appbox.model.entity.DataFieldModel;
import appbox.model.entity.EntityMemberModel;
import appbox.model.entity.EntityRefModel;
import appbox.model.entity.IndexModelBase;
import appbox.store.query.ISqlSelectQuery;
import com.alibaba.fastjson.JSON;
import com.github.jasync.sql.db.Connection;
import com.github.jasync.sql.db.pool.ConnectionPool;
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection;
import com.github.jasync.sql.db.postgresql.PostgreSQLConnectionBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class PgSqlStore extends SqlStore implements AutoCloseable {

    private final ConnectionPool<PostgreSQLConnection> _connectionPool;

    public PgSqlStore(String settings) {
        String connectionString = null;
        if (settings.startsWith("{")) { //TODO:暂简单判断
            var s = JSON.parseObject(settings, SqlStoreSettings.class);
            connectionString = String.format("jdbc:postgresql://%s:%s/%s?user=%s&password=%s"
                    , s.Host, s.Port, s.Database, s.User, s.Password);
        } else { //only for test
            connectionString = settings;
        }
        _connectionPool = PostgreSQLConnectionBuilder.createConnectionPool(connectionString);
    }

    @Override
    protected char nameEscaper() { return '"';}

    //region ====Connection & Transaction====
    @Override
    protected CompletableFuture<Connection> openConnection() {
        return _connectionPool.take().thenApply(c -> c);
    }

    @Override
    protected void closeConnection(Connection connection) {
        _connectionPool.giveBack((PostgreSQLConnection) connection);
    }

    @Override
    public CompletableFuture<DbTransaction> beginTransaction() {
        return _connectionPool.take()
                .thenCompose(c -> c.sendQuery("BEGIN")
                        .thenApply(r -> new DbTransaction(c, this::giveBack)));
    }

    private void giveBack(Connection connection) {
        _connectionPool.giveBack((PostgreSQLConnection) connection);
    }

    @Override
    public void close() throws Exception {
        _connectionPool.disconnect().get();
    }
    //endregion

    //region ====DDL Methods====

    @Override
    protected List<DbCommand> makeCreateTable(EntityModel model, IDesignContext ctx) {
        var tableName = model.getSqlTableName(false, ctx);
        var fks       = new ArrayList<CharSequence>(); //引用外键集合

        var sb = new StringBuilder(200);
        sb.append("CREATE TABLE \"");
        sb.append(tableName);
        sb.append("\" (");

        boolean needSep = false;
        for (var m : model.getMembers()) {
            if (needSep)
                sb.append(',');
            else
                needSep = true;

            if (m.type() == EntityMemberModel.EntityMemberType.DataField) {
                buildFieldDefine((DataFieldModel) m, sb, false);
            } else if (m.type() == EntityMemberModel.EntityMemberType.EntityRef) {
                var rm = (EntityRefModel) m;
                if (!rm.isAggregationRef()) { //只有非聚合引合创建外键
                    fks.add(buildForeignKey(rm, ctx, tableName));
                    //考虑旧实现CreateGetTreeNodeChildsDbFuncCommand
                }
            }
        }
        sb.append(");");

        //加入EntityRef引用外键
        for (var fk : fks) {
            sb.append(fk);
        }

        var res = new ArrayList<DbCommand>();
        var cmd = new DbCommand();
        cmd.setCommandText(sb.toString());
        res.add(cmd);

        //Build Indexes
        buildIndexes(model, res, tableName);

        return res;
    }

    //endregion

    //region ====DML Methods====
    @Override
    public DbCommand buildQuery(ISqlSelectQuery query) {
        return PgSqlQueryBuilder.build(query);
    }
    //endregion

    //region ====Helper Methods====
    private static String getActionRuleString(EntityRefModel.EntityRefActionRule rule) {
        switch (rule) {
            case Cascade:
                return "CASCADE";
            case SetNull:
                return "SET NULL";
            default:
                return "NO ACTION";
        }
    }

    private static String buildFieldDefine(DataFieldModel dfm, StringBuilder sb, boolean forAlter) {
        var     defaultValue   = "";
        boolean noDefaultValue = dfm.defaultValue() == null;
        var     fieldName      = forAlter ? dfm.sqlColOriginalName() : dfm.sqlColName();
        sb.append("\"");
        sb.append(fieldName);
        sb.append("\" ");
        if (forAlter)
            sb.append("TYPE ");

        switch (dfm.dataType()) {
            case String:
                defaultValue = noDefaultValue ? "''" : "'" + dfm.defaultValue() + "'";
                sb.append(dfm.length() == 0 ? "text " : "varchar(" + dfm.length() + ") ");
                break;
            case DateTime:
                defaultValue = noDefaultValue ? "'1970-1-1'" : "'" + dfm.defaultValue() + "'";
                sb.append("timestamptz ");
                break;
            case Bool:
                defaultValue = noDefaultValue ? "false" : dfm.defaultValue().toString();
                sb.append("bool ");
                break;
            case Byte:
            case Short:
                defaultValue = noDefaultValue ? "0" : dfm.defaultValue().toString();
                sb.append("int2 ");
                break;
            case Enum:
            case Int:
                defaultValue = noDefaultValue ? "0" : dfm.defaultValue().toString();
                sb.append("int4 ");
                break;
            case Long:
                defaultValue = noDefaultValue ? "0" : dfm.defaultValue().toString();
                sb.append("int8 ");
                break;
            case Decimal:
                defaultValue = noDefaultValue ? "0" : dfm.defaultValue().toString();
                sb.append("decimal(");
                sb.append((dfm.length() + dfm.decimals()));
                sb.append(',');
                sb.append(dfm.decimals());
                sb.append(") ");
                break;
            case Guid:
                defaultValue = noDefaultValue ? "'00000000-0000-0000-0000-000000000000'" : "'" + defaultValue + "'";
                sb.append("uuid ");
                break;
            case Float:
                defaultValue = noDefaultValue ? "0" : dfm.defaultValue().toString();
                sb.append("float4 ");
                break;
            case Double:
                defaultValue = noDefaultValue ? "0" : dfm.defaultValue().toString();
                sb.append("float8 ");
                break;
            case Binary:
                sb.append("bytea ");
                break;
            default:
                throw new RuntimeException("不支持的字段类型:" + dfm.dataType().name());
        }

        if (!dfm.allowNull() && !forAlter) {
            if (dfm.dataType() == DataFieldModel.DataFieldType.Binary)
                throw new RuntimeException("Binary field must be allow null");
            sb.append("NOT NULL DEFAULT ");
            sb.append(defaultValue);
        }

        return defaultValue;
    }

    private static CharSequence buildForeignKey(EntityRefModel rm, IDesignContext ctx, String tableName) {
        var refModel = ctx.getEntityModel(rm.getRefModelIds().get(0));
        //使用模型标识+成员标识作为fk name以减少重命名带来的影响
        var fkName = String.format("FK_%s_%s"
                , Long.toUnsignedString(rm.owner.id()), Short.toUnsignedInt(rm.memberId()));
        var sb = new StringBuilder(50);

        sb.append("ALTER TABLE \"");
        sb.append(tableName);
        sb.append("\" ADD CONSTRAINT \"");
        sb.append(fkName);
        sb.append("\" FOREIGN KEY (");
        for (int i = 0; i < rm.getFKMemberIds().length; i++) {
            var fk = (DataFieldModel) rm.owner.getMember(rm.getFKMemberIds()[i]);
            if (i != 0)
                sb.append(',');
            sb.append("\"");
            sb.append(fk.sqlColName());
            sb.append("\"");
        }

        sb.append(") REFERENCES \"");
        sb.append(refModel.getSqlTableName(false, ctx)); //引用目标使用新名称
        sb.append("\" (");
        for (int i = 0; i < refModel.sqlStoreOptions().primaryKeys().size(); i++) {
            var pk = (DataFieldModel) refModel.getMember(refModel.sqlStoreOptions().primaryKeys().get(i).memberId);
            if (i != 0)
                sb.append(',');
            sb.append("\"");
            sb.append(pk.sqlColName());
            sb.append("\"");
        }

        //TODO:pg's MATCH SIMPLE?
        sb.append(") ON UPDATE ");
        sb.append(getActionRuleString(rm.updateRule()));
        sb.append(" ON DELETE ");
        sb.append(getActionRuleString(rm.deleteRule()));
        sb.append(';');

        return sb;
    }

    private static void buildIndexes(EntityModel model, List<DbCommand> commands, String tableName) {
        if (!model.sqlStoreOptions().hasIndexes())
            return;

        if (model.persistentState() != PersistentState.Detached) {
            var deletedIndexes = model.sqlStoreOptions().getIndexes().stream()
                    .filter(i -> i.persistentState() == PersistentState.Deleted)
                    .toArray(IndexModelBase[]::new);
            for (var index : deletedIndexes) {
                var cmdTxt = String.format("DROP INDEX IF EXISTS \"IX_%s_%s\""
                        , Long.toUnsignedString(model.id()), Byte.toUnsignedInt(index.indexId()));
                var cmd = new DbCommand();
                cmd.setCommandText(cmdTxt);
                commands.add(cmd);
            }
        }

        var newIndexes = model.sqlStoreOptions().getIndexes().stream()
                .filter(i -> i.persistentState() == PersistentState.Detached)
                .toArray(IndexModelBase[]::new);
        for (var index : newIndexes) {
            var sb = new StringBuilder(50);
            sb.append("CREATE ");
            if (index.unique())
                sb.append("UNIQUE ");
            sb.append("INDEX \"IX_");
            sb.append(Long.toUnsignedString(model.id()));
            sb.append('_');
            sb.append(Byte.toUnsignedInt(index.indexId()));
            sb.append("\" ON \"");
            sb.append(tableName);
            sb.append("\" (");
            for (int i = 0; i < index.fields().length; i++) {
                if (i != 0)
                    sb.append(',');
                var dfm = (DataFieldModel) model.getMember(index.fields()[i].memberId);
                sb.append("\"");
                sb.append(dfm.sqlColName());
                sb.append("\"");
                if (index.fields()[i].orderByDesc)
                    sb.append(" DESC");
            }
            sb.append(')');

            var cmd = new DbCommand();
            cmd.setCommandText(sb.toString());
            commands.add(cmd);
        }

        //TODO:处理改变的索引
    }
    //endregion

}
