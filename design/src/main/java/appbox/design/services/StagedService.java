package appbox.design.services;

import appbox.entities.StagedModel;
import appbox.logging.Log;
import appbox.model.EntityModel;
import appbox.model.ModelBase;
import appbox.model.ModelFolder;
import appbox.runtime.RuntimeContext;
import appbox.serialization.BinSerializer;
import appbox.store.EntityStore;
import appbox.store.KVTransaction;
import appbox.store.query.TableScan;
import appbox.store.utils.ModelCodeUtil;
import appbox.utils.IdUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.javatuples.Quartet;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 管理设计时临时保存的尚未发布的模型及相关代码
 */
public final class StagedService {


    public static CompletableFuture<byte[]> loadCodeDataAsync(long modelId) {
        var devId = RuntimeContext.current().currentSession().leafOrgUnitId();
        var q     = new TableScan<>(IdUtil.SYS_STAGED_MODEL_ID, StagedModel.class);
        q.where(StagedModel.DEVELOPER.eq(devId)
                .and(StagedModel.MODEL.eq(Long.toUnsignedString(modelId)))
                .and(StagedModel.TYPE.eq(StagedType.SourceCode.value)));

        return q.toListAsync().thenCompose(r -> {

            if (ObjectUtils.isEmpty(r)) {
                return CompletableFuture.completedFuture(null);
            } else {
                return CompletableFuture.completedFuture(r.get(0).getData());
            }
        });
    }

    /**
     * 保存Staged模型
     * @param model
     * @return
     */
    public static CompletableFuture<Void> saveModelAsync(ModelBase model) {
        var data = BinSerializer.serialize(model, false, null);
        return saveAsync(StagedType.Model, String.valueOf(model.id()), data);
    }

    /**
     * 保存模型类型的根目录
     * @param folder
     * @return
     */
    public static CompletableFuture<Void> saveFolderAsync(ModelFolder folder) {
        if (folder.getParent() != null)
            throw new RuntimeException("仅允许保存模型类型的根目录");
        var data = BinSerializer.serialize(folder, false, null);
        return saveAsync(StagedType.Folder, folder.getAppId() + "-" + folder.getTargetModelType().value /*不要使用folder.Id*/, data);
    }

    /**
     * 专用于保存服务模型代码
     * @param modelId
     * @param sourceCode
     * @return
     */
    public static CompletableFuture<Void> saveServiceCodeAsync(long modelId, String sourceCode) {
        var data = ModelCodeUtil.encodeServiceCode(sourceCode, false);
        return saveAsync(StagedType.SourceCode, String.valueOf(modelId), data);
    }

    /**
     * 专用于加载服务模型代码
     * @param serviceModelId
     * @return
     */
    public static CompletableFuture<String> loadServiceCode(long serviceModelId) {
        var developerID = RuntimeContext.current().currentSession().leafOrgUnitId();

        var q = new TableScan<>(IdUtil.SYS_STAGED_MODEL_ID, StagedModel.class);
        q.where(StagedModel.TYPE.eq(StagedType.SourceCode.value)
                .and(StagedModel.MODEL.eq(Long.toUnsignedString(serviceModelId)))
                .and(StagedModel.DEVELOPER.eq(developerID)));

        return q.toListAsync().thenCompose(r -> {
            if (ObjectUtils.isEmpty(r))
                return CompletableFuture.completedFuture(null);

            return CompletableFuture.completedFuture(ModelCodeUtil.decodeServiceCode(r.get(0).getData()).sourceCode);
        });
    }

    public static CompletableFuture<Void> saveReportCodeAsync(long modelId, String code) {
        var data = ModelCodeUtil.compressCode(code);
        return saveAsync(StagedType.SourceCode, String.valueOf(modelId), data);
    }

    public static CompletableFuture<String> loadReportCodeAsync(long modelId) {
        return loadCodeDataAsync(modelId).thenCompose(r -> {
            if (r == null) {
                return CompletableFuture.completedFuture(null);
            } else {
                return CompletableFuture.completedFuture(ModelCodeUtil.decompressCode(r));
            }
        });
    }

    /**
     * 专用于保存视图模型代码
     * @param modelId
     * @param templateCode
     * @param scriptCode
     * @param styleCode
     * @return
     */
    public static CompletableFuture<Void> saveViewCodeAsync(long modelId, String templateCode, String scriptCode, String styleCode) {
        var data = ModelCodeUtil.encodeViewCode(templateCode, scriptCode, styleCode);
        return saveAsync(StagedType.SourceCode, String.valueOf(modelId), data);
    }

    public static CompletableFuture<Quartet<Boolean, String, String, String>> loadViewCodeAsync(long modelId) {
        return loadCodeDataAsync(modelId).thenCompose(r -> {
            if (r == null) {return CompletableFuture.completedFuture(Quartet.with(false, null, null, null));}
            else {
                Map res =ModelCodeUtil.decodeViewCode(r);
                return CompletableFuture.completedFuture(Quartet.with(false, (String)res.get("templateCode"),(String) res.get("scriptCode"), (String)res.get("styleCode")));
            }
        });
    }

    public static CompletableFuture<Void> saveViewRuntimeCodeAsync(long modelId, String runtimeCode) {
        if (StringUtils.isEmpty(runtimeCode)){
            return CompletableFuture.completedFuture(null);
        }else{
            var data = ModelCodeUtil.encodeViewRuntimeCode(runtimeCode);
            return saveAsync(StagedType.ViewRuntimeCode, String.valueOf(modelId), data);
        }
    }

    public static CompletableFuture<String> loadViewRuntimeCode(long viewModelId) {
        var developerID = RuntimeContext.current().currentSession().leafOrgUnitId();
        var q = new TableScan<>(IdUtil.SYS_STAGED_MODEL_ID,StagedModel.class);
        q.where(StagedModel.TYPE.eq(StagedType.ViewRuntimeCode.value)
                .and(StagedModel.MODEL.eq(Long.toUnsignedString(viewModelId)))
                .and(StagedModel.DEVELOPER.eq(developerID)));
        return q.toListAsync().thenCompose(r -> {
            if (ObjectUtils.isEmpty(r)) {
                return CompletableFuture.completedFuture(null);
            } else {
                String str=ModelCodeUtil.decodeViewRuntimeCode(r.get(0).getData());
                return CompletableFuture.completedFuture(str);
            }
        });
    }

    public static CompletableFuture<Void> saveAsync(StagedType type, String modelIdString, byte[] data) {
        var developerId   = RuntimeContext.current().currentSession().leafOrgUnitId();
        EntityModel model = RuntimeContext.current().getModel(IdUtil.SYS_STAGED_MODEL_ID);

        //String modelIdString = Long.toUnsignedString(modelId); //转换为字符串

        //TODO:暂采用先读取再插入的方式，待实现KVUpsert后改写
        var q = new TableScan<>(IdUtil.SYS_STAGED_MODEL_ID, StagedModel.class);
        q.where(StagedModel.TYPE.eq(type.value)
                .and(StagedModel.MODEL.eq(modelIdString))
                .and(StagedModel.DEVELOPER.eq(developerId))
        );
        return q.toListAsync().thenApply(list -> {
            if (list.size() > 1) Log.warn("Detected multi row");
            StagedModel stagedItem = list.size() > 0 ? list.get(0) : new StagedModel();
            stagedItem.setType(type.value);
            stagedItem.setModelId(modelIdString);
            stagedItem.setDeveloperId(developerId);
            stagedItem.setData(data);
            return stagedItem;
        }).thenCompose(item -> EntityStore.insertEntityAsync(item, true));
    }

    /**
     * 加载挂起项目
     */
    public static CompletableFuture<StagedItems> loadStagedAsync(boolean onlyModelsAndFolders) {
        var developerId   = RuntimeContext.current().currentSession().leafOrgUnitId();
        var q = new TableScan<>(IdUtil.SYS_STAGED_MODEL_ID, StagedModel.class);
        if (onlyModelsAndFolders)
            q.where(StagedModel.TYPE.le(StagedType.Folder.value)
                            .and(StagedModel.DEVELOPER.eq(developerId)));
        else
            q.where(StagedModel.DEVELOPER.eq(developerId));
        return q.toListAsync().thenApply(r-> new StagedItems(r));
    }

    /**
     * 发布时删除当前会话下所有挂起
     * @return
     */
    public static CompletableFuture<Void> deleteStagedAsync() {
        var developerId   = RuntimeContext.current().currentSession().leafOrgUnitId();
        var model = (EntityModel)RuntimeContext.current().getModel(IdUtil.SYS_STAGED_MODEL_ID);

        var q = new TableScan<>(IdUtil.SYS_STAGED_MODEL_ID, StagedModel.class);
        q.where(StagedModel.DEVELOPER.eq(developerId));
        return q.toListAsync().thenCompose(res->{
            if (ObjectUtils.isNotEmpty(res)) {
                return KVTransaction.beginAsync().thenCompose(txn ->{
                    CompletableFuture future=null;
                    for (StagedModel stagedModel : res) {
                        if (future == null) {
                            future = EntityStore.deleteEntityAsync(model, stagedModel.id(), txn);
                        } else {
                            future = future.thenCompose(r -> EntityStore.deleteEntityAsync(model, stagedModel.id(), txn));
                        }
                    }
                    return future;
                });
            }else{
                return CompletableFuture.completedFuture(null);
            }
        });
    }

    /**
     * 删除挂起的模型及相关
     * @param modelId
     * @return
     */
    public static CompletableFuture<Void> deleteModelAsync(long modelId) {
        var developerId   = RuntimeContext.current().currentSession().leafOrgUnitId();
        var q = new TableScan<>(IdUtil.SYS_STAGED_MODEL_ID, StagedModel.class);
        q.where(StagedModel.MODEL.eq(Long.toUnsignedString(modelId))
                .and(StagedModel.DEVELOPER.eq(developerId))
        );
        return q.toListAsync().thenCompose(res->{
            if (ObjectUtils.isNotEmpty(res)) {
                return KVTransaction.beginAsync().thenCompose(txn ->{
                    CompletableFuture future=null;
                    for (StagedModel stagedModel : res) {
                        if (future == null) {
                            future = EntityStore.deleteEntityAsync(stagedModel);
                        } else {
                            future = future.thenCompose(r -> EntityStore.deleteEntityAsync(stagedModel));
                        }
                    }
                    return future;
                });
            }else{
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
