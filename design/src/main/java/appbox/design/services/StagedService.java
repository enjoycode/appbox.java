package appbox.design.services;

import appbox.entities.StagedModel;
import appbox.logging.Log;
import appbox.model.EntityModel;
import appbox.model.ModelBase;
import appbox.model.ModelFolder;
import appbox.runtime.RuntimeContext;
import appbox.store.EntityStore;
import appbox.store.query.TableScan;
import appbox.store.utils.ModelCodeUtil;
import appbox.utils.IdUtil;
import org.javatuples.Quartet;

import java.util.concurrent.CompletableFuture;

/**
 * 管理设计时临时保存的尚未发布的模型及相关代码
 */
public final class StagedService {


    public static CompletableFuture<byte[]> loadCodeDataAsync(long modelId){
        return null;
    }

    /**
     * 保存Staged模型
     * @param model
     * @return
     */
    public static CompletableFuture<Void> saveModelAsync(ModelBase model)
    {
        return null;
    }

    /**
     * 保存模型类型的根目录
     * @param folder
     * @return
     */
    public static CompletableFuture<Void> saveFolderAsync(ModelFolder folder)
    {
        return null;
    }

    /**
     * 专用于保存服务模型代码
     * @param modelId
     * @param sourceCode
     * @return
     */
    public static CompletableFuture<Void> saveServiceCodeAsync(long modelId, String sourceCode)
    {
        return null;

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

        return q.toListAsync().thenApply(res -> {
            if (res == null || res.size() <= 0)
                return null;

            return ModelCodeUtil.decodeServiceCode(res.get(0).getData()).sourceCode;
        });
    }

    public static CompletableFuture<Void> saveReportCodeAsync(long modelId, String code)
    {
        return null;
    }

    public static CompletableFuture<String> loadReportCodeAsync(long modelId)
    {
      return null;
    }
    /**
     * 专用于保存视图模型代码
     * @param modelId
     * @param templateCode
     * @param scriptCode
     * @param styleCode
     * @return
     */
    public static CompletableFuture<Void> saveViewCodeAsync(long modelId, String templateCode, String scriptCode, String styleCode){
        return null;
    }

    public static CompletableFuture<Quartet<Boolean,String,String,String>> loadViewCodeAsync(long modelId){
        return null;
    }

    public static CompletableFuture<Void> saveViewRuntimeCodeAsync(long modelId, String runtimeCode){
        return null;
    }

    public static CompletableFuture<String> loadViewRuntimeCode(long viewModelId){
        return null;
    }

    public static CompletableFuture<Void> saveAsync(StagedType type, long modelId, byte[] data) {
        var         developerId = RuntimeContext.current().currentSession().leafOrgUnitId();
        EntityModel model       = RuntimeContext.current().getModel(IdUtil.SYS_STAGED_MODEL_ID);

        String modelIdString = Long.toUnsignedString(modelId); //转换为字符串

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
    public static CompletableFuture<StagedItems> loadStagedAsync(boolean onlyModelsAndFolders){
        return null;
    }
    /**
     * 发布时删除当前会话下所有挂起
     * @return
     */
    public static CompletableFuture<Void> deleteStagedAsync(){
        return null;
    }

    /**
     * 删除挂起的模型及相关
     * @param modelId
     * @return
     */
    public static CompletableFuture<Void> deleteModelAsync(long modelId){
        return null;
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
                case 0: return Model;
                case 1: return Folder;
                case 2: return SourceCode;
                case 3: return ViewRuntimeCode;
                default: throw new RuntimeException();
            }
        }
    }

}
