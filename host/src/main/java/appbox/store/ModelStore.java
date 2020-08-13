package appbox.store;

import appbox.core.model.ApplicationModel;
import appbox.server.channel.messages.NewAppRequire;

import java.util.concurrent.CompletableFuture;

/**
 * 模型存储实现
 */
public final class ModelStore {

    /**
     * 创建新的应用
     */
    public static CompletableFuture<Byte> createApplicationAsync(ApplicationModel app) {
        return SysStoreApi.createApplicationAsync(app);
    }

}
