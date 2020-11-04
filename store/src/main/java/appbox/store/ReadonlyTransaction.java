package appbox.store;

/**
 * 快照一致性只读事务
 */
public final class ReadonlyTransaction implements IKVTransaction {

    //TODO:缓存同一事务加载的数据, 分为EntityCache及EntityMemberCache

    @Override
    public KVTxnId id() {
        return null;
    }

}
