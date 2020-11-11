package appbox.store;

import com.github.jasync.sql.db.Connection;

import java.util.function.Consumer;

/** 包装关系数据库事务 */
public class DbTransaction implements AutoCloseable {

    private final Connection _connection;
    private final Consumer<Connection> _giveBackToPool;

    protected DbTransaction(Connection connection, Consumer<Connection> giveBackToPool) {
        _connection = connection;
        _giveBackToPool = giveBackToPool;
    }

    protected Connection getConnection() { return _connection; }

    @Override
    public void close() throws Exception {
        _giveBackToPool.accept(_connection);
    }
}
