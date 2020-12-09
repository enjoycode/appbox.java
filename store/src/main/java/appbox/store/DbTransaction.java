package appbox.store;

import appbox.logging.Log;
import com.github.jasync.sql.db.Connection;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** 包装关系数据库事务 */
public class DbTransaction implements AutoCloseable {

    private final Connection           _connection;
    private final Consumer<Connection> _giveBackToPool;
    private final AtomicInteger        _status = new AtomicInteger(0);

    protected DbTransaction(Connection connection, Consumer<Connection> giveBackToPool) {
        _connection     = connection;
        _giveBackToPool = giveBackToPool;
    }

    protected Connection getConnection() { return _connection; }

    public CompletableFuture<Void> commitAsync() {
        if (_status.compareAndExchange(0, 1) != 0) {
            throw new RuntimeException("DbTransaction has committed or rollback");
        }

        return _connection.sendQuery("COMMIT").thenAccept(r -> {
            _giveBackToPool.accept(_connection);
        });
    }

    public void rollback() {
        if (_status.compareAndExchange(0, 2) != 0) {
            Log.debug("DbTransaction has commit or rollback.");
            return;
        }

        _connection.sendQuery("ROLLBACK").thenAccept(r -> {
            _giveBackToPool.accept(_connection);
        });
    }

    @Override
    public void close() throws Exception {
        rollback();
    }
}
