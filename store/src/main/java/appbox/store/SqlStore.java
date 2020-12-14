package appbox.store;

import appbox.data.SqlEntity;
import appbox.design.IDesignContext;
import appbox.logging.Log;
import appbox.model.DataStoreModel;
import appbox.model.EntityModel;
import appbox.model.entity.DataFieldModel;
import appbox.model.entity.EntityMemberModel;
import appbox.serialization.IEntityMemberWriter;
import appbox.store.query.ISqlSelectQuery;
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
                    var model = ModelStore.loadModelAsync(storeId).get();
                    if (model == null)
                        throw new RuntimeException("DataStoreModel not exists");
                    var dataStoreModel = (DataStoreModel) model;
                    //TODO:**** 根据Provider创建相应的实例
                    store = new PgSqlStore(dataStoreModel.settings());
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

    //region ====DML Methods====
    protected DbCommand buildInsertCommand(SqlEntity entity, EntityModel model) {
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

    public final CompletableFuture<Void> insertAsync(SqlEntity entity, DbTransaction txn) {
        if (entity == null)
            throw new IllegalArgumentException();
        //TODO:判断持久化状态

        var model = entity.model();
        if (model.sqlStoreOptions() == null)
            throw new UnsupportedOperationException("Can't insert entity to sqlstore");

        var cmd = buildInsertCommand(entity, model);
        CompletableFuture<Connection> connectionFuture =
                txn == null ? openConnection() : CompletableFuture.completedFuture(txn.getConnection());
        return connectionFuture.thenCompose(cmd::execNonQueryAsync)
                .handle((res, ex) -> {
                    //仅关闭非事务内的连接
                    if (txn == null && cmd.connection != null /*可能未打开就出现异常*/) {
                        closeConnection(cmd.connection);
                    }
                    if (ex != null) { //TODO:友好错误信息
                        Log.warn(String.format("Insert to [%s] error:\n%s", model.name(), ex.getMessage()));
                        throw new RuntimeException(ex); //需要重新抛出异常
                    }
                    return null;
                });
    }
    //endregion

}
