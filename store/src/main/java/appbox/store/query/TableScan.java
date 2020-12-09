package appbox.store.query;

import appbox.channel.messages.KVScanEntityRequest;
import appbox.channel.messages.KVScanEntityResponse;
import appbox.data.SysEntity;
import appbox.data.SysEntityKVO;
import appbox.expressions.Expression;
import appbox.model.EntityModel;
import appbox.runtime.RuntimeContext;
import appbox.store.EntityStore;
import appbox.store.ReadonlyTransaction;
import appbox.store.SysStoreApi;
import appbox.utils.IdUtil;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class TableScan<T extends SysEntity> extends KVScan {
    private final Class<T>       _clazz;
    private       EntityModel    _model;
    private       Constructor<T> _ctor;

    public TableScan(long modelId, Class<T> clazz) {
        super(modelId);
        _clazz = clazz;
    }

    //region ====skip & take methods====
    public TableScan<T> skip(int rows) {
        this.skip = rows;
        return this;
    }

    public TableScan<T> take(int rows) {
        this.take = rows;
        return this;
    }
    //endregion

    //region ====where methods====
    public TableScan<T> where(Expression filter) {
        this.filter = filter;
        return this;
    }
    //endregion

    //region ====toXXX methods====
    private T createInstance() {
        try {
            if (_ctor == null) {
                if (_clazz == SysEntityKVO.class)
                    _ctor = _clazz.getDeclaredConstructor(EntityModel.class);
                else
                    _ctor = _clazz.getDeclaredConstructor();
            }
            return _clazz == SysEntityKVO.class ? _ctor.newInstance(_model) : _ctor.newInstance();
        } catch (Exception ex) {
            throw new RuntimeException("Can't create instance.");
        }
    }

    private CompletableFuture<KVScanEntityResponse<T>> execPartScanAsync(long raftGroupId, int skip, int take) {
        if (raftGroupId == 0)
            return CompletableFuture.completedFuture(new KVScanEntityResponse<>(null));

        var req = new KVScanEntityRequest(raftGroupId, skip, take, filter);
        return SysStoreApi.execKVScanAsync(req, new KVScanEntityResponse<>(this::createInstance));
    }

    public CompletableFuture<List<T>> toListAsync(/*ITransaction txn*/) {
        var app = RuntimeContext.current().getApplicationModel(IdUtil.getAppIdFromModelId(modelId));
        _model = RuntimeContext.current().getModel(modelId);

        //先判断是否需要快照读事务 //TODO:跨分区也需要
        //ReadonlyTransaction txn = rootIncluder == null ? null : new ReadonlyTransaction();
        ReadonlyTransaction txn = null;

        //根据是否分区执行不同的查询
        if (_model.sysStoreOptions().hasPartitionKeys()) {
            throw new RuntimeException("未实现");
        } else {
            return EntityStore.getOrCreateGlobalTablePartition(app, _model, txn)
                    .thenCompose(raftGroupId -> execPartScanAsync(raftGroupId, skip, take))
                    .thenApply(res -> res.result); //TODO:处理Includes
        }
    }
    //endregion

}
