package appbox.store;

import appbox.model.ApplicationModel;

import java.util.concurrent.CompletableFuture;

/**
 * 模型存储实现
 */
public final class ModelStore {

    /**
     * 创建新的应用，成功返回应用对应的存储Id
     */
    public static CompletableFuture<Byte> createApplicationAsync(ApplicationModel app) {
        return SysStoreApi.metaNewAppAsync(app).thenApply(r -> {
            if (r.errorCode != 0) {
                throw new SysStoreException(r.errorCode);
            }
            return r.appId;
        });
    }

}
