package appbox.design.services;

import appbox.entities.StagedModel;
import appbox.logging.Log;
import appbox.model.EntityModel;
import appbox.model.ModelBase;
import appbox.model.ModelFolder;
import appbox.runtime.RuntimeContext;
import appbox.serialization.IBinSerializable;
import appbox.store.EntityStore;
import appbox.store.KVTransaction;
import appbox.store.ViewCode;
import appbox.store.query.TableScan;
import appbox.store.utils.ModelCodeUtil;
import appbox.utils.IdUtil;

import java.util.concurrent.CompletableFuture;

/**
 * 管理设计时临时保存的尚未发布的模型及相关代码
 */
public final class StagedService {

    private static CompletableFuture<byte[]> loadCodeDataAsync(long modelId) {
        var devId = RuntimeContext.current().currentSession().leafOrgUnitId();
        var q     = new TableScan<>(IdUtil.SYS_STAGED_MODEL_ID, StagedModel.class);
        q.where(StagedModel.DEVELOPER.eq(devId)
                .and(StagedModel.MODEL.eq(Long.toUnsignedString(modelId)))
                .and(StagedModel.TYPE.eq(StagedType.SourceCode.value)));

        return q.toListAsync().thenApply(r -> {
            if (r == null || r.size() == 0) {
                return null;
            } else {
                return r.get(0).getData();
            }
        });
    }

    /** 保存Staged模型 */
    public static CompletableFuture<Void> saveModelAsync(ModelBase model) {
        var data = IBinSerializable.serialize(model, false);
        return saveAsync(StagedType.Model, Long.toUnsignedString(model.id()), data);
    }

    /** 保存模型类型的根目录 */
    public static CompletableFuture<Void> saveFolderAsync(ModelFolder folder) {
        if (folder.getParent() != null)
            throw new RuntimeException("仅允许保存模型类型的根目录");
        var data = IBinSerializable.serialize(folder, false);
        return saveAsync(StagedType.Folder,
                folder.appId() + "-" + folder.targetModelType().value /*不要使用folder.Id*/, data);
    }

    /** 专用于保存服务模型代码 */
    public static CompletableFuture<Void> saveServiceCodeAsync(long modelId, String sourceCode) {
        var data = ModelCodeUtil.encodeServiceCode(sourceCode, false);
        return saveAsync(StagedType.SourceCode, Long.toUnsignedString(modelId), data);
    }

    /** 专用于加载服务模型代码 */
    public static CompletableFuture<String> loadServiceCode(long serviceModelId) {
        return loadCodeDataAsync(serviceModelId).thenApply(data -> {
            if (data == null)
                return null;
            return ModelCodeUtil.decodeServiceCode(data).sourceCode;
        });
    }

    //public static CompletableFuture<Void> saveReportCodeAsync(long modelId, String code) {
    //    var data = ModelCodeUtil.compressCode(code);
    //    return saveAsync(StagedType.SourceCode, Long.toUnsignedString(modelId), data);
    //}
    //
    //public static CompletableFuture<String> loadReportCodeAsync(long modelId) {
    //    return loadCodeDataAsync(modelId).thenCompose(r -> {
    //        if (r == null) {
    //            return CompletableFuture.completedFuture(null);
    //        } else {
    //            return CompletableFuture.completedFuture(ModelCodeUtil.decompressCode(r));
    //        }
    //    });
    //}

    /** 专用于保存视图模型代码 */
    public static CompletableFuture<Void> saveViewCodeAsync(long modelId, String templateCode, String scriptCode, String styleCode) {
        var data = ModelCodeUtil.encodeViewCode(templateCode, scriptCode, styleCode);
        return saveAsync(StagedType.SourceCode, Long.toUnsignedString(modelId), data);
    }

    public static CompletableFuture<ViewCode> loadViewCodeAsync(long viewModelId) {
        return loadCodeDataAsync(viewModelId).thenApply(data -> {
            if (data == null)
                return null;
            return ModelCodeUtil.decodeViewCode(data);
        });
    }

    public static CompletableFuture<Void> saveViewRuntimeCodeAsync(long modelId, String runtimeCode) {
        if (runtimeCode == null || runtimeCode.equals("")) {
            return CompletableFuture.completedFuture(null);
        } else {
            var data = ModelCodeUtil.encodeViewRuntimeCode(runtimeCode);
            return saveAsync(StagedType.ViewRuntimeCode, Long.toUnsignedString(modelId), data);
        }
    }

    //public static CompletableFuture<String> loadViewRuntimeCode(long viewModelId) {
    //    var developerID = RuntimeContext.current().currentSession().leafOrgUnitId();
    //    var q = new TableScan<>(IdUtil.SYS_STAGED_MODEL_ID,StagedModel.class);
    //    q.where(StagedModel.TYPE.eq(StagedType.ViewRuntimeCode.value)
    //            .and(StagedModel.MODEL.eq(Long.toUnsignedString(viewModelId)))
    //            .and(StagedModel.DEVELOPER.eq(developerID)));
    //    return q.toListAsync().thenCompose(r -> {
    //        if (ObjectUtils.isEmpty(r)) {
    //            return CompletableFuture.completedFuture(null);
    //        } else {
    //            String str=ModelCodeUtil.decodeViewRuntimeCode(r.get(0).getData());
    //            return CompletableFuture.completedFuture(str);
    //        }
    //    });
    //}

    private static CompletableFuture<Void> saveAsync(StagedType type, String modelIdString, byte[] data) {
        var developerId = RuntimeContext.current().currentSession().leafOrgUnitId();

        //TODO:暂采用先读取再插入的方式，待实现KVUpsert后改写
        var q = new TableScan<>(IdUtil.SYS_STAGED_MODEL_ID, StagedModel.class);
        q.where(StagedModel.TYPE.eq(type.value)
                .and(StagedModel.MODEL.eq(modelIdString))
                .and(StagedModel.DEVELOPER.eq(developerId))
        );
        return q.toListAsync().thenApply(list -> {
            if (list != null && list.size() > 1) Log.warn("Detected multi row");
            StagedModel stagedItem = list != null && list.size() > 0 ? list.get(0) : new StagedModel();
            stagedItem.setType(type.value);
            stagedItem.setModelId(modelIdString);
            stagedItem.setDeveloperId(developerId);
            stagedItem.setData(data);
            return stagedItem;
        }).thenCompose(item -> EntityStore.insertEntityAsync(item, true));
    }

    /**
     * 加载挂起项目
     * @param onlyModelsAndFolders true用于DesignTree加载; false用于发布时加载
     */
    public static CompletableFuture<StagedItems> loadStagedAsync(boolean onlyModelsAndFolders) {
        var developerId = RuntimeContext.current().currentSession().leafOrgUnitId();
        var q           = new TableScan<>(IdUtil.SYS_STAGED_MODEL_ID, StagedModel.class);
        if (onlyModelsAndFolders)
            q.where(StagedModel.TYPE.le(StagedType.Folder.value)
                    .and(StagedModel.DEVELOPER.eq(developerId)));
        else
            q.where(StagedModel.DEVELOPER.eq(developerId));
        return q.toListAsync().thenApply(StagedItems::new);
    }

    /** 发布时删除当前会话下所有挂起 */
    public static CompletableFuture<Void> deleteStagedAsync() {
        var developerId = RuntimeContext.current().currentSession().leafOrgUnitId();
        var model       = (EntityModel) RuntimeContext.current().getModel(IdUtil.SYS_STAGED_MODEL_ID);

        var q = new TableScan<>(IdUtil.SYS_STAGED_MODEL_ID, StagedModel.class);
        q.where(StagedModel.DEVELOPER.eq(developerId));
        return q.toListAsync().thenCompose(res -> {
            if (res != null && res.size() > 0) {
                return KVTransaction.beginAsync().thenCompose(txn -> {
                    CompletableFuture<Void> future = null;
                    for (StagedModel stagedModel : res) {
                        if (future == null) {
                            future = EntityStore.deleteEntityAsync(model, stagedModel.id(), txn);
                        } else {
                            future = future.thenCompose(r -> EntityStore.deleteEntityAsync(model, stagedModel.id(), txn));
                        }
                    }
                    return future.thenCompose(r -> txn.commitAsync());
                });
            } else {
                return CompletableFuture.completedFuture(null);
            }
        });
    }

    /** 删除挂起的模型及相关 */
    public static CompletableFuture<Void> deleteModelAsync(long modelId) {
        var developerId = RuntimeContext.current().currentSession().leafOrgUnitId();
        var q           = new TableScan<>(IdUtil.SYS_STAGED_MODEL_ID, StagedModel.class);
        q.where(StagedModel.MODEL.eq(Long.toUnsignedString(modelId))
                .and(StagedModel.DEVELOPER.eq(developerId))
        );
        return q.toListAsync().thenCompose(res -> {
            if (res != null && res.size() > 0) {
                return KVTransaction.beginAsync().thenCompose(txn -> {
                    CompletableFuture<Void> future = null;
                    for (StagedModel stagedModel : res) {
                        if (future == null) {
                            future = EntityStore.deleteEntityAsync(stagedModel);
                        } else {
                            future = future.thenCompose(r -> EntityStore.deleteEntityAsync(stagedModel));
                        }
                    }
                    return future.thenCompose(r -> txn.commitAsync());
                });
            } else {
                return CompletableFuture.completedFuture(null);
            }
        });
    }

    enum StagedType {
        Model(0),           //模型序列化数据
        Folder(1),          //文件夹
        SourceCode(2),      //服务模型或视图模型的源代码 //TODO:考虑按类型分开
        ViewRuntimeCode(3); //仅用于视图模型前端编译好的运行时脚本代码

        public final byte value;

        StagedType(int value) {
            this.value = (byte) value;
        }

        public static StagedType from(byte value) {
            switch (value) {
                case 0:
                    return Model;
                case 1:
                    return Folder;
                case 2:
                    return SourceCode;
                case 3:
                    return ViewRuntimeCode;
                default:
                    throw new RuntimeException();
            }
        }
    }

}
