package sys;

import java.util.concurrent.CompletableFuture;

public final class SqlStore {
    private SqlStore() {}

    public CompletableFuture<DbTransaction> beginTransaction() {return null;}

    public CompletableFuture<Void> insertAsync(SqlEntityBase entity, DbTransaction txn) {return null;}

    public CompletableFuture<Void> updateAsync(SqlEntityBase entity, DbTransaction txn) {return null;}

    public CompletableFuture<Void> deleteAsync(SqlEntityBase entity, DbTransaction txn) {return null;}
}
