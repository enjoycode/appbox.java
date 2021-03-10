package appbox.store;

import appbox.data.PersistentState;
import appbox.data.SqlEntity;
import appbox.design.IDesignContext;
import appbox.logging.Log;
import appbox.model.DataStoreModel;
import appbox.model.EntityModel;
import appbox.model.entity.DataFieldModel;
import appbox.model.entity.EntityMemberModel;
import appbox.model.entity.FieldWithOrder;
import appbox.serialization.IEntityMemberWriter;
import appbox.store.query.ISqlSelectQuery;
import appbox.store.query.SqlDeleteCommand;
import appbox.store.query.SqlRowReader;
import appbox.store.query.SqlUpdateCommand;
import com.github.jasync.sql.db.Connection;
import com.github.jasync.sql.db.QueryResult;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class SqlStore {

    //region ====statics=====
    private static final HashMap<Long, SqlStore> sqlStores = new HashMap<>();

    /** only for test */
    public static void inject(long storeId, SqlStore store) {
        sqlStores.putIfAbsent(storeId, store);
    }

    public static SqlStore get(long storeId) {
        var store = sqlStores.get(storeId);
        if (store == null) {
            synchronized (sqlStores) {
                //load from meta store
                store = sqlStores.get(storeId);
                if (store != null)
                    return store;

                try {
                    var model = ModelStore.loadDataStoreAsync(storeId).get();
                    if (model == null)
                        throw new RuntimeException("DataStoreModel not exists");
                    //TODO:**** 检查类型并根据Provider创建相应的实例
                    store = new PgSqlStore(model.settings());
                    sqlStores.put(storeId, store);
                } catch (Exception ex) {
                    Log.error("Load DataStoreModel error: " + ex.getMessage());
                }
            }
        }
        return store;
    }
    //endregion

    /** 名称转义符，如PG用引号包括字段名称\"xxx\" */
    protected abstract char nameEscaper();

    //region ====Connection & Transaction====
    protected abstract CompletableFuture<Connection> openConnection();

    protected abstract void closeConnection(Connection connection);

    public abstract CompletableFuture<DbTransaction> beginTransaction();
    //endregion

    //region ====DDL Methods====
    protected abstract List<DbCommand> makeCreateTable(EntityModel model, IDesignContext ctx);

    protected abstract List<DbCommand> makeAlterTable(EntityModel model, IDesignContext ctx);

    protected abstract DbCommand makeDropTable(EntityModel model, IDesignContext ctx);

    public final CompletableFuture<Void> createTableAsync(EntityModel model, DbTransaction txn, IDesignContext ctx) {
        var cmds = makeCreateTable(model, ctx);

        CompletableFuture<Long> task = null;
        for (var cmd : cmds) {
            if (task == null)
                task = cmd.execNonQueryAsync(txn.getConnection());
            else
                task = task.thenCompose(r -> cmd.execNonQueryAsync(txn.getConnection()));
        }
        return task.thenAccept(r -> {});
    }

    public final CompletableFuture<Void> alterTableAsync(EntityModel model, DbTransaction txn, IDesignContext ctx) {
        var cmds = makeAlterTable(model, ctx);

        CompletableFuture<Long> task = CompletableFuture.completedFuture(0L); //目前cmds可能为空
        for (var cmd : cmds) {
            task = task.thenCompose(r -> cmd.execNonQueryAsync(txn.getConnection()));
        }
        return task.thenAccept(r -> {});
    }

    public final CompletableFuture<Void> dropTableAsync(EntityModel model, DbTransaction txn, IDesignContext ctx) {
        var cmd = makeDropTable(model, ctx);
        return cmd.execNonQueryAsync(txn.getConnection()).thenAccept(r -> {});
    }
    //endregion

    //region ====DML Insert/Update/Delete Entity Methods====

    /** 根据Entity及其模型生成相应的Insert命令 */
    protected DbCommand buildInsertCommand(SqlEntity entity, EntityModel model) {
        //TODO: cache SqlText to EntityModel's SqlStoreOptions
        //注意目前实现仅插入非空的字段，并且不缓存命令
        var cmd = new DbCommand();
        var sb  = new StringBuilder(100);
        sb.append("Insert Into ");
        sb.append(nameEscaper());
        sb.append(model.getSqlTableName(false, null));
        sb.append(nameEscaper());
        sb.append(" (");

        int parasCount = 0; //用于判断有没有写入字段值
        for (var member : model.getMembers()) {
            if (member.type() != EntityMemberModel.EntityMemberType.DataField)
                continue;

            var dataField = (DataFieldModel) member;
            entity.writeMember(dataField.memberId(), cmd, IEntityMemberWriter.SF_NONE);
            if (cmd.getParameters().size() > parasCount) {
                if (parasCount != 0)
                    sb.append(',');
                sb.append(nameEscaper());
                sb.append(dataField.name());
                sb.append(nameEscaper());
                parasCount = cmd.getParameters().size();
            }
        }
        sb.append(") Values (");
        for (int i = 0; i < cmd.getParameters().size(); i++) {
            if (i != 0)
                sb.append(',');
            sb.append('?');
        }
        sb.append(')');

        cmd.setCommandText(sb.toString());
        return cmd;
    }

    /** 根据Entity及其模型生成相应的Update命令 */
    protected DbCommand buildUpdateCommand(SqlEntity entity, EntityModel model) {
        var cmd       = new DbCommand();
        var tableName = model.getSqlTableName(false, null);
        var sb        = new StringBuilder(100);

        sb.append("Update ");
        sb.append(nameEscaper());
        sb.append(tableName);
        sb.append(nameEscaper());
        sb.append(" Set ");

        boolean        hasChangedMember = false;
        DataFieldModel dfm              = null;
        for (var mm : model.getMembers()) {
            if (mm.type() != EntityMemberModel.EntityMemberType.DataField)
                continue;
            dfm = (DataFieldModel) mm;
            if (dfm.isPrimaryKey()) //跳过主键
                continue;
            //TODO:跳过未改变值的字段

            entity.writeMember(mm.memberId(), cmd, IEntityMemberWriter.SF_NONE);

            if (hasChangedMember) {
                sb.append(',');
            } else {
                hasChangedMember = true;
            }

            sb.append(nameEscaper());
            sb.append(dfm.sqlColName());
            sb.append(nameEscaper());
            sb.append("=?");
        }

        if (!hasChangedMember)
            throw new RuntimeException("entity without changed");

        //根据主键生成条件
        sb.append(" Where ");
        buildWhereForUpdateOrDeleteEntity(entity, model, cmd, sb);

        cmd.setCommandText(sb.toString());
        return cmd;
    }

    /** 根据Entity及其模型生成相应的Delete命令 */
    protected DbCommand buildDeleteCommand(SqlEntity entity, EntityModel model) {
        var cmd       = new DbCommand();
        var tableName = model.getSqlTableName(false, null);
        var sb        = new StringBuilder(50);

        sb.append("Delete From ");
        sb.append(nameEscaper());
        sb.append(tableName);
        sb.append(nameEscaper());

        sb.append(" Where ");
        //根据主键生成条件 TODO:没有主键是否直接抛异常
        buildWhereForUpdateOrDeleteEntity(entity, model, cmd, sb);

        cmd.setCommandText(sb.toString());
        return cmd;
    }

    public final CompletableFuture<Void> insertAsync(SqlEntity entity, DbTransaction txn) {
        if (entity == null || entity.persistentState() != PersistentState.Detached) {
            tryRollbackTxn(txn);
            throw new UnsupportedOperationException();
        }
        //不需要判断model.sqlStoreOptions() == null，参数已经限制了类型

        var model = entity.model();
        var cmd   = buildInsertCommand(entity, model);
        CompletableFuture<Connection> getConnection =
                txn == null ? openConnection() : CompletableFuture.completedFuture(txn.getConnection());
        return getConnection.thenCompose(cmd::execNonQueryAsync)
                .handle((res, ex) -> {
                    handleDbCommandResult(txn, cmd, ex);
                    return null;
                });
    }

    /** 仅适用于更新具备主键的实体，否则使用SqlUpdateCommand明确字段及条件更新 */
    public final CompletableFuture<Void> updateAsync(SqlEntity entity, DbTransaction txn) {
        if (entity == null || entity.persistentState() != PersistentState.Modified) {
            tryRollbackTxn(txn);
            throw new UnsupportedOperationException();
        }

        var model = entity.model();
        if (!model.sqlStoreOptions().hasPrimaryKeys()) {
            tryRollbackTxn(txn);
            throw new UnsupportedOperationException("Can't update entity without primary key");
        }

        var cmd = buildUpdateCommand(entity, model);
        CompletableFuture<Connection> getConnection =
                txn == null ? openConnection() : CompletableFuture.completedFuture(txn.getConnection());
        return getConnection.thenCompose(cmd::execNonQueryAsync)
                .handle((res, ex) -> {
                    handleDbCommandResult(txn, cmd, ex);
                    return null;
                });
    }

    /** 仅适用于删除具备主键的实体，否则使用SqlDeleteCommand明确指定条件删除 */
    public final CompletableFuture<Void> deleteAsync(SqlEntity entity, DbTransaction txn) {
        if (entity == null || entity.persistentState() != PersistentState.Deleted) {
            tryRollbackTxn(txn);
            throw new UnsupportedOperationException();
        }

        var model = entity.model();
        if (!model.sqlStoreOptions().hasPrimaryKeys()) {
            tryRollbackTxn(txn);
            throw new UnsupportedOperationException("Can't delete entity without primary key");
        }

        var cmd = buildDeleteCommand(entity, model);
        CompletableFuture<Connection> getConnection =
                txn == null ? openConnection() : CompletableFuture.completedFuture(txn.getConnection());
        return getConnection.thenCompose(cmd::execNonQueryAsync)
                .handle((res, ex) -> {
                    handleDbCommandResult(txn, cmd, ex);
                    return null;
                });
    }

    /** 根据实体持久化状态调用相应的Insert/Update/Delete */
    public final CompletableFuture<Void> saveAsync(SqlEntity entity, DbTransaction txn) {
        switch (entity.persistentState()) {
            case Detached:
                return insertAsync(entity, txn);
            case Modified:
                return updateAsync(entity, txn);
            case Deleted:
                return deleteAsync(entity, txn);
            default:
                return CompletableFuture.completedFuture(null);
        }
    }

    //endregion

    //region ====DML Update/Delete Command Methods====
    protected abstract DbCommand buildUpdateCommand(SqlUpdateCommand updateCommand);

    protected abstract DbCommand buildDeleteCommand(SqlDeleteCommand deleteCommand);

    public final CompletableFuture<Long> execUpdateAsync(SqlUpdateCommand updateCommand, DbTransaction txn) {
        //暂不支持无条件更新，以防止误操作
        if (updateCommand.getFilter() == null) {
            tryRollbackTxn(txn);
            throw new RuntimeException("SqlUpdateCommand must has where condition");
        }

        var cmd = buildUpdateCommand(updateCommand);
        CompletableFuture<Connection> getConnection =
                txn == null ? openConnection() : CompletableFuture.completedFuture(txn.getConnection());
        if (updateCommand.hasOutputs()) {
            return getConnection.thenCompose(cmd::execQueryAsync).handle((res, ex) -> {
                handleDbCommandResult(txn, cmd, ex);
                var rows      = res.getRows();
                var rowReader = new SqlRowReader(rows.columnNames());
                for (var row : rows) {
                    rowReader.rowData = row;
                    updateCommand.readOutputs(rowReader);
                }
                return res.getRowsAffected();
            });
        } else {
            return getConnection.thenCompose(cmd::execNonQueryAsync).handle((res, ex) -> {
                handleDbCommandResult(txn, cmd, ex);
                return res;
            });
        }
    }

    public final CompletableFuture<Long> execDeleteAsync(SqlDeleteCommand deleteCommand, DbTransaction txn) {
        //暂不支持无条件更新，以防止误操作
        if (deleteCommand.getFilter() == null) {
            tryRollbackTxn(txn);
            throw new RuntimeException("SqlDeleteCommand must has where condition");
        }

        var cmd = buildDeleteCommand(deleteCommand);
        CompletableFuture<Connection> getConnection =
                txn == null ? openConnection() : CompletableFuture.completedFuture(txn.getConnection());
        return getConnection.thenCompose(cmd::execNonQueryAsync).handle((res, ex) -> {
            handleDbCommandResult(txn, cmd, ex);
            return res;
        });
    }
    //endregion

    //region ====DML Query Methods====
    protected abstract DbCommand buildQuery(ISqlSelectQuery query);

    public final CompletableFuture<QueryResult> runQuery(ISqlSelectQuery query) {
        var cmd        = buildQuery(query);
        var connFuture = openConnection();
        return connFuture.thenCompose(cmd::execQueryAsync)
                .handle((res, ex) -> {
                    //仅关闭非事务内的连接
                    if (/*txn == null &&*/ cmd.connection != null /*可能未打开就出现异常*/) {
                        closeConnection(cmd.connection);
                    }
                    if (ex != null) {
                        Log.warn(String.format("Query [%s] error:\n%s", cmd.getCommandText(), ex.getMessage()));
                        throw new RuntimeException(ex); //需要重新抛出异常
                    }
                    return res;
                });
    }
    //endregion

    //region ====Helper Methods====

    /** 删除或更新实体时根据主键生成相应的条件 */
    private void buildWhereForUpdateOrDeleteEntity(SqlEntity entity, EntityModel model,
                                                   DbCommand cmd, StringBuilder sb) {
        FieldWithOrder pk = null;
        DataFieldModel mm = null;
        for (int i = 0; i < model.sqlStoreOptions().primaryKeys().length; i++) {
            pk = model.sqlStoreOptions().primaryKeys()[i];
            mm = (DataFieldModel) model.getMember(pk.memberId);

            entity.writeMember(pk.memberId, cmd, IEntityMemberWriter.SF_NONE);

            if (i != 0)
                sb.append(" And");
            sb.append(" ");
            sb.append(nameEscaper());
            sb.append(mm.sqlColName());
            sb.append(nameEscaper());
            sb.append("=?");
        }
    }

    private void handleDbCommandResult(DbTransaction txn, DbCommand cmd, Throwable ex) {
        //仅关闭非事务内的连接
        if (txn == null && cmd.connection != null /*可能未打开就出现异常*/) {
            closeConnection(cmd.connection);
        }
        if (ex != null) { //TODO:友好错误信息
            tryRollbackTxn(txn);
            Log.warn(String.format("Run cmd:[%s] error:\n%s", cmd.getCommandText(), ex.getMessage()));
            throw new RuntimeException(ex); //需要重新抛出异常
        }
    }

    private static void tryRollbackTxn(DbTransaction txn) {
        if (txn != null) {
            txn.rollback();
        }
    }
    //endregion

}
