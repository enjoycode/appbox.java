package appbox.store;

import appbox.channel.messages.KVInsertModelRequire;
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

    public static CompletableFuture<Void> insertModelAsync(ModelBase model) {
        var req = new KVInsertModelRequire();
        req.model = model;

        return SysStoreApi.execKVInsertAsync(req).thenAccept(r -> {
           checkStoreError(r);
        });
    }

}
