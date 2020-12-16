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
import com.github.jasync.sql.db.ConnectionPoolConfiguration;
import com.github.jasync.sql.db.pool.ConnectionPool;
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection;
import com.github.jasync.sql.db.postgresql.PostgreSQLConnectionBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class PgSqlStore extends SqlStore implements AutoCloseable {

    private final ConnectionPool<PostgreSQLConnection> _connectionPool;

    public PgSqlStore(String settings) {
        if (settings.startsWith("{")) { //TODO:暂简单判断
            var s = JSON.parseObject(settings, SqlStoreSettings.class);
            //connectionString = String.format("jdbc:postgresql://%s:%s/%s?user=%s&password=%s"
            //        , s.Host, s.Port, s.Database, s.User, s.Password);
            var poolConfig = new ConnectionPoolConfiguration(s.Host, Integer.parseInt(s.Port)
                    , s.Database, s.User, s.Password, 100);
            _connectionPool = PostgreSQLConnectionBuilder.createConnectionPool(poolConfig);
        } else { //only for test
            _connectionPool = PostgreSQLConnectionBuilder.createConnectionPool(settings);
        }
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

        //Build Create Table
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

        //Build PrimaryKey
        if (model.sqlStoreOptions().hasPrimaryKeys()) {
            //使用模型标识作为PK名称以避免重命名影响
            sb.append("ALTER TABLE \"");
            sb.append(tableName);
            sb.append("\" ADD CONSTRAINT \"PK_");
            sb.append(Long.toUnsignedString(model.id()));
            sb.append("\" PRIMARY KEY (");
            for (int i = 0; i < model.sqlStoreOptions().primaryKeys().length; i++) {
                var mm = (DataFieldModel) model.getMember(model.sqlStoreOptions().primaryKeys()[i].memberId);
                if (i != 0)
                    sb.append(',');
                sb.append("\"");
                sb.append(mm.sqlColName());
                sb.append("\"");
            }
            sb.append(");");
        }

        //加入EntityRef引用外键
        for (var fk : fks) {
            sb.append(fk);
        }

        var res = new ArrayList<DbCommand>();
        res.add(new DbCommand(sb.toString()));

        //Build Indexes
        buildIndexes(model, res, tableName);

        return res;
    }

    @Override
    protected List<DbCommand> makeAlterTable(EntityModel model, IDesignContext ctx) {
        //TODO:***处理主键变更

        String tableName = model.getSqlTableName(false, ctx);

        StringBuilder   sb          = new StringBuilder(200);
        boolean         needCommand = false; //用于判断是否需要处理NpgsqlCommand
        List<String>    fks         = new ArrayList<>(); //引用外键列表
        List<DbCommand> commands    = new ArrayList<>();
        //先处理表名称有没有变更，后续全部使用新名称
        if (model.isNameChanged()) {
            String oldTableName = model.getSqlTableName(true, ctx);
            commands.add(new DbCommand(String.format("ALTER TABLE \"%s\" RENAME TO \"%s\"", oldTableName, tableName)));
        }

        //处理删除的成员
        var deletedMembers = model.getMembers().stream()
                .filter(t -> t.persistentState() == PersistentState.Deleted)
                .toArray(EntityMemberModel[]::new);
        if (deletedMembers.length > 0) {
            //#region ----删除的成员----
            for (EntityMemberModel m : deletedMembers) {
                if (m.type() == EntityMemberModel.EntityMemberType.DataField) {
                    needCommand = true;
                    sb.append(String.format("ALTER TABLE \"%s\" DROP COLUMN \"%s\";", tableName, ((DataFieldModel) m).sqlColOriginalName()));
                } else if (m.type() == EntityMemberModel.EntityMemberType.EntityRef) {
                    EntityRefModel rm = (EntityRefModel) m;
                    if (!rm.isAggregationRef()) {
                        String fkName = String.format("FK_%s_%s", rm.owner.id(), rm.memberId()); //TODO:特殊处理DbFirst导入表的外键约束名称
                        fks.add(String.format("ALTER TABLE \"%s\" DROP CONSTRAINT \"%s\";", tableName, fkName));
                    }
                }
            }

            String cmdText = sb.toString();
            if (needCommand) {
                //加入删除的外键SQL
                for (String fk : fks) {
                    sb.insert(0, fk);
                    sb.append("\n");
                }
                commands.add(new DbCommand(cmdText));
            }
            //#endregion
        }

        //reset
        needCommand = false;
        fks.clear();

        //处理新增的成员
        var addedMembers = model.getMembers().stream()
                .filter(t -> t.persistentState() == PersistentState.Detached)
                .toArray(EntityMemberModel[]::new);
        if (addedMembers.length > 0) {
            //#region ----新增的成员----
            for (EntityMemberModel m : addedMembers) {
                if (m.type() == EntityMemberModel.EntityMemberType.DataField) {
                    needCommand = true;
                    sb.append(String.format("ALTER TABLE \"%s\" ADD COLUMN ", tableName));
                    buildFieldDefine((DataFieldModel) m, sb, false);
                    sb.append(";");
                } else if (m.type() == EntityMemberModel.EntityMemberType.EntityRef) {
                    EntityRefModel rm = (EntityRefModel) m;
                    if (!rm.isAggregationRef()) //只有非聚合引合创建外键
                    {
                        fks.add(buildForeignKey(rm, ctx, tableName).toString());
                        //考虑CreateGetTreeNodeChildsDbFuncCommand
                    }
                }
            }

            String cmdText = sb.toString();
            if (needCommand) {
                //加入关系
                sb.append("\n");
                for (String fk : fks) {
                    sb.append(fk).append("\n");
                }

                commands.add(new DbCommand(cmdText));
            }
            //#endregion
        }

        //reset
        needCommand = false;
        fks.clear();

        //处理修改的成员
        var changedMembers = model.getMembers().stream()
                .filter(t -> t.persistentState() == PersistentState.Modified)
                .toArray(EntityMemberModel[]::new);
        if (changedMembers.length > 0) {
            //#region ----修改的成员----
            for (EntityMemberModel m : changedMembers) {
                if (m.type() == EntityMemberModel.EntityMemberType.DataField) {
                    DataFieldModel dfm = (DataFieldModel) m;
                    //先处理数据类型变更，变更类型或者变更AllowNull或者变更默认值
                    if (dfm.isDataTypeChanged()) {
                        sb.append(String.format("ALTER TABLE \"%s\" ALTER COLUMN ", tableName));
                        String defaultValue = buildFieldDefine(dfm, sb, true);

                        if (dfm.allowNull()) {
                            sb.append(String.format(",ALTER COLUMN \"%s\" DROP NOT NULL", dfm.sqlColOriginalName()));
                        } else {
                            if (dfm.dataType() == DataFieldModel.DataFieldType.Binary)
                                throw new RuntimeException("Binary field must be allow null");
                            sb.append(String.format(",ALTER COLUMN \"%s\" SET NOT NULL,ALTER COLUMN \"%s\" SET DEFAULT %s", dfm.sqlColOriginalName(), dfm.sqlColOriginalName(), defaultValue));
                        }
                        commands.add(new DbCommand(sb.toString()));
                    }

                    //再处理重命名列
                    if (m.isNameChanged()) {
                        commands.add(new DbCommand(String.format("ALTER TABLE \"%s\" RENAME COLUMN \"%s\" TO \"%s\"", tableName, dfm.sqlColOriginalName(), dfm.sqlColName())));
                    }
                }

                //TODO:处理EntityRef更新与删除规则
                //注意不再需要同旧实现一样变更EntityRef的外键约束名称 "ALTER TABLE \"XXX\" RENAME CONSTRAINT \"XXX\" TO \"XXX\""
                //因为ModelFirst的外键名称为FK_{MemberId}；CodeFirst为导入的名称
            }
            //#endregion
        }

        //处理索引变更
        buildIndexes(model, commands, tableName);

        return commands;
    }

    @Override
    protected DbCommand makeDropTable(EntityModel model, IDesignContext ctx) {
        String tableName = model.getSqlTableName(true, ctx);
        return new DbCommand(String.format("DROP TABLE IF EXISTS \"%s\"", tableName));
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
        for (int i = 0; i < refModel.sqlStoreOptions().primaryKeys().length; i++) {
            var pk = (DataFieldModel) refModel.getMember(refModel.sqlStoreOptions().primaryKeys()[i].memberId);
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
                commands.add(new DbCommand(cmdTxt));
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

            commands.add(new DbCommand(sb.toString()));
        }

        //TODO:处理改变的索引
    }
    //endregion

}
