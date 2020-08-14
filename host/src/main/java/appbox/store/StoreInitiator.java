package appbox.store;

import appbox.core.logging.Log;
import appbox.core.model.ApplicationModel;

import java.util.concurrent.CompletableFuture;

/**
 * 系统存储初始化，仅用于启动集群第一节点时
 */
public final class StoreInitiator {

    public static CompletableFuture<Boolean> initAsync() {
        //TODO:考虑判断是否已初始化
        Log.debug("Start init system store...");
        //新建sys应用
        var app = new ApplicationModel("appbox", "sys");
        return ModelStore.createApplicationAsync(app).thenApply(appStoreId -> {
            app.setAppStoreId(appStoreId);
            return true;
        });
    }

}
