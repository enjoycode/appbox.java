package appbox.store;

import appbox.store.query.ISqlSelectQuery;
import com.github.jasync.sql.db.Connection;
import com.github.jasync.sql.db.pool.ConnectionPool;
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection;
import com.github.jasync.sql.db.postgresql.PostgreSQLConnectionBuilder;

import java.util.concurrent.CompletableFuture;

public final class PgSqlStore extends SqlStore implements AutoCloseable {

    private final ConnectionPool<PostgreSQLConnection> _connectionPool;

    public PgSqlStore(String settings) {
        //TODO:
        _connectionPool = PostgreSQLConnectionBuilder.createConnectionPool(settings);
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
        return _connectionPool.take().thenApply(c -> new DbTransaction(c, this::giveBack));
    }

    private void giveBack(Connection connection) {
        _connectionPool.giveBack((PostgreSQLConnection) connection);
    }

    @Override
    public void close() throws Exception {
        _connectionPool.disconnect().get();
    }
    //endregion

    @Override
    public DbCommand buildQuery(ISqlSelectQuery query) {
        return PgSqlQueryBuilder.build(query);
    }

}
