package appbox.store.query;

import appbox.channel.messages.KVScanEntityRequest;
import appbox.channel.messages.KVScanEntityResponse;
import appbox.data.EntityId;
import appbox.data.SqlEntity;
import appbox.data.SysEntity;
import appbox.data.SysEntityKVO;
import appbox.entities.EntityMemberValueGetter;
import appbox.expressions.EntityExpression;
import appbox.expressions.EntityPathExpression;
import appbox.expressions.EntitySetExpression;
import appbox.expressions.Expression;
import appbox.model.EntityModel;
import appbox.model.entity.EntityRefModel;
import appbox.model.entity.EntitySetModel;
import appbox.runtime.RuntimeContext;
import appbox.serialization.IEntityMemberWriter;
import appbox.store.EntityStore;
import appbox.store.ReadonlyTransaction;
import appbox.store.SysStoreApi;
import appbox.utils.IdUtil;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class TableScan<T extends SysEntity> extends KVScan {
    private final Class<T>         _clazz;
    private       EntityModel      _model;
    private       Constructor<T>   _ctor;
    private       EntityExpression _t; //TODO:待修改toTreeAsync()实现后移除

    public TableScan(long modelId, Class<T> clazz) {
        super(modelId);
        _clazz = clazz;
    }

    public EntityPathExpression m(String name) { //TODO:待修改toTreeAsync()实现后移除
        if (_t == null)
            _t = new EntityExpression(modelId, this);
        return _t.m(name);
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

    //TODO: 参数Function<T,EntityId> getFK, BiComsumer<T,T> setParent
    public CompletableFuture<List<T>> toTreeAsync(Function<TableScan<T>, Expression> childrenMember) {
        filter = null; //TODO:暂忽略条件

        return toListAsync().thenApply(list -> {
            var children         = (EntitySetExpression) childrenMember.apply(this);
            var childrenModel    = (EntitySetModel) _model.getMember(children.name);
            var treeParentMember = (EntityRefModel) _model.getMember(childrenModel.refMemberId());

            var tree   = new ArrayList<T>(list.size());
            var getter = new EntityMemberValueGetter();
            //TODO:暂简单实现，待优化为排序后处理
            for (var obj : list) {
                obj.writeMember(treeParentMember.getFKMemberIds()[0], getter, IEntityMemberWriter.SF_NONE);
                var fk = getter.value;
                if (fk == null) {
                    tree.add(obj);
                } else {
                    var parent = list.stream().filter(t -> t.id().equals(fk)).findFirst();
                    if (parent.isPresent()) {
                        //TODO:*** set child.Parent = parent
                        @SuppressWarnings("unchecked")
                        var childrenList = (List<T>) parent.get().getNaviPropForFetch(childrenModel.name());
                        childrenList.add(obj);
                    } else {
                        tree.add(obj);
                    }
                }
            }

            return tree;
        });
    }
    //endregion

}
