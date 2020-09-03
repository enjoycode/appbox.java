package appbox.store;

import appbox.channel.messages.KVInsertModelCodeRequire;
import appbox.channel.messages.KVInsertModelRequire;
import appbox.channel.messages.KVScanModelsRequest;
import appbox.channel.messages.StoreResponse;
import appbox.model.ApplicationModel;
import appbox.model.ModelBase;

import java.util.concurrent.CompletableFuture;

/**
 * 模型存储实现
 */
public final class ModelStore {

    private static <T extends StoreResponse> void checkStoreError(T response) throws SysStoreException {
        if (response.errorCode != 0) {
            throw new SysStoreException(response.errorCode);
        }
    }

    /**
     * 创建新的应用，成功返回应用对应的存储Id
     */
    public static CompletableFuture<Byte> createApplicationAsync(ApplicationModel app) {
        return SysStoreApi.metaNewAppAsync(app).thenApply(r -> {
            checkStoreError(r);
            return r.appId;
        });
    }

    public static CompletableFuture<Void> insertModelAsync(ModelBase model, KVTransaction txn) {
        var req = new KVInsertModelRequire();
        req.model = model;
        req.txnId.copyFrom(txn.id());

        return SysStoreApi.execKVInsertAsync(req).thenAccept(r -> checkStoreError(r));
    }

    //region ====模型代码及Assembly相关操作====

    /**
     * 保存模型相关的代码，目前主要用于服务模型及视图模型
     *
     * @param codeData 已经压缩编码过
     */
    public static CompletableFuture<Void> upsertModelCodeAsync(long modelId, byte[] codeData, KVTransaction txn) {
        var req = new KVInsertModelCodeRequire();
        req.modelId  = modelId;
        req.codeData = codeData;
        req.txnId.copyFrom(txn.id());

        return SysStoreApi.execKVInsertAsync(req).thenAccept(r -> checkStoreError(r));
    }
    //endregion

    //region ====Read Methods====

    /**
     * 用于设计时加载所有ApplicationModel
     */
    public static CompletableFuture<ApplicationModel[]> loadAllApplicationAsync() {
        var req = new KVScanModelsRequest(KVScanModelsRequest.ModelsType.Applications);
        return SysStoreApi.execKVScanAsync(req).thenApply(r -> (ApplicationModel[]) r.result);
    }

    /**
     * 用于设计时加载所有Model
     */
    public static CompletableFuture<ModelBase[]> loadAllModelAsync() {
        var req = new KVScanModelsRequest(KVScanModelsRequest.ModelsType.Models);
        return SysStoreApi.execKVScanAsync(req).thenApply(r -> (ModelBase[]) r.result);
    }
    //endregion

}
