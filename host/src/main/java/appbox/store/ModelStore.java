package appbox.store;

import appbox.core.model.ApplicationModel;

import java.util.concurrent.CompletableFuture;

/**
 * 模型存储实现
 */
public final class ModelStore {

    /**
     * 创建新的应用，成功返回应用对应的存储Id
     */
    public static CompletableFuture<Byte> createApplicationAsync(ApplicationModel app) {
        return SysStoreApi.createApplicationAsync(app);
    }

}