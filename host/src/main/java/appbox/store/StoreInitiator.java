package appbox.store;

import appbox.core.logging.Log;

import java.util.concurrent.CompletableFuture;

/**
 * 系统存储初始化，仅用于启动集群第一节点时
 */
public final class StoreInitiator {

    public static CompletableFuture<Boolean> initAsync() {
        Log.debug("Start init system store...");
        return CompletableFuture.completedFuture(true);
    }

}
