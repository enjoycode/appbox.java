package appbox.store.query;

import appbox.channel.messages.KVGetIndexRequest;
import appbox.channel.messages.KVGetIndexResponse;
import appbox.data.SysEntity;
import appbox.data.SysUniqueIndex;
import appbox.expressions.KVFieldExpression;
import appbox.model.EntityModel;
import appbox.model.entity.SysIndexModel;
import appbox.runtime.RuntimeContext;
import appbox.store.EntityStore;
import appbox.store.SysStoreApi;

import java.lang.reflect.ParameterizedType;
import java.util.concurrent.CompletableFuture;

public final class IndexGet<E extends SysEntity, T extends SysUniqueIndex<E>> { //TODO: rename to UniqueIndexGet

    private final Class<E>            _entityClass;
    private final Class<T>            _indexClass;
    private       SysIndexModel       _indexModel;
    private final KVFieldExpression[] _fields;
    private final Object[]            _values;

    public IndexGet(Class<T> indexClass) {
        _indexClass  = indexClass;
        _entityClass = (Class<E>) ((ParameterizedType) _indexClass.getGenericSuperclass()).getActualTypeArguments()[0];
        long modelId = 0;
        byte indexId = 0;
        try {
            modelId = (long) _entityClass.getDeclaredField("MODELID").get(null);
            indexId = (byte) indexClass.getDeclaredField("INDEXID").get(null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        EntityModel model = RuntimeContext.current().getModel(modelId);
        for (var idx : model.sysStoreOptions().getIndexes()) {
            if (idx.indexId() == indexId) {
                _indexModel = idx;
                break;
            }
        }

        _fields = new KVFieldExpression[_indexModel.fields().length];
        _values = new Object[_indexModel.fields().length];
    }

    public IndexGet<E, T> where(KVFieldExpression field, Object value) {
        for (int i = 0; i < _indexModel.fields().length; i++) {
            if (_indexModel.fields()[i].memberId == field.fieldId) {
                _fields[i] = field;
                _values[i] = value;
                return this;
            }
        }
        throw new RuntimeException("Only for indexed field");
    }

    private void validatePredicates() {
        //TODO:验证不允许为null的key没有设置条件值
        for (var field : _fields) {
            if (field == null) {
                throw new RuntimeException("Must set index field value");
            }
        }
    }

    public CompletableFuture<T> toIndexRowAsync() {
        validatePredicates();

        //TODO:暂只支持非分区表惟一索引
        if (_indexModel.isGlobal() || _indexModel.owner.sysStoreOptions().hasPartitionKeys()) {
            throw new RuntimeException("未实现");
        }

        var app = RuntimeContext.current().getApplicationModel(_indexModel.owner.appId());
        //定位目标分区
        return EntityStore.getOrCreateGlobalTablePartition(app, _indexModel.owner, null)
                .thenCompose(groupId -> {
                    if (groupId == 0) {
                        return CompletableFuture.completedFuture(null);
                    }

                    var req = new KVGetIndexRequest(groupId, _indexModel, _fields, _values);
                    return SysStoreApi.execKVGetAsync(req, new KVGetIndexResponse<>(_indexClass));
                }).thenApply(res -> res == null ? null : res.getRow()); //TODO:回填条件字段值
    }

    public CompletableFuture<E> toEntityAsync() {
        //TODO:*****临时简单实现（两次读）
        return toIndexRowAsync().thenCompose(row -> EntityStore.loadAsync(_entityClass, row.getTargetId()));
    }

}
