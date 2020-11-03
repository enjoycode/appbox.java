package appbox.store;

import appbox.channel.messages.*;
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
     * @param codeData 已经压缩编码过
     */
    public static CompletableFuture<Void> upsertModelCodeAsync(long modelId, byte[] codeData, KVTransaction txn) {
        var req = new KVInsertModelCodeRequire();
        req.modelId  = modelId;
        req.codeData = codeData;
        req.txnId.copyFrom(txn.id());

        return SysStoreApi.execKVInsertAsync(req).thenAccept(r -> checkStoreError(r));
    }

    /**
     * 仅用于加载服务模型的代码
     */
    public static CompletableFuture<ServiceCode> loadServiceCodeAsync(long modelId) {
        var req = new KVGetModelRequest(modelId, (byte) 3);
        return SysStoreApi.execKVGetAsync(req).thenApply(r -> (ServiceCode) r.result);
    }
    //endregion

    //region ====Read Methods====

    /**
     * 用于设计时加载所有ApplicationModel
     */
    public static CompletableFuture<ApplicationModel[]> loadAllApplicationAsync() {
        var req = new KVScanModelsRequest(true);
        return SysStoreApi.execKVScanAsync(req).thenApply(r -> (ApplicationModel[]) r.result);
    }

    /**
     * 用于运行时加载单个应用模型
     */
    public static CompletableFuture<ApplicationModel> loadApplicationAsync(int appId) {
        var req = new KVGetModelRequest(appId, (byte) 1);
        return SysStoreApi.execKVGetAsync(req).thenApply(r -> (ApplicationModel) r.result);
    }

    /**
     * 用于设计时加载所有Model
     */
    public static CompletableFuture<ModelBase[]> loadAllModelAsync() {
        var req = new KVScanModelsRequest(false);
        return SysStoreApi.execKVScanAsync(req).thenApply(r -> (ModelBase[]) r.result);
    }

    /**
     * 用于运行时加载单个模型
     */
    public static CompletableFuture<ModelBase> loadModelAsync(long modelId) {
        var req = new KVGetModelRequest(modelId, (byte) 2);
        return SysStoreApi.execKVGetAsync(req).thenApply(r -> (ModelBase) r.result);
    }
    //endregion

}
