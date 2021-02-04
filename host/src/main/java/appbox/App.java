package appbox;

import appbox.logging.Log;
import appbox.runtime.RuntimeContext;
import appbox.server.runtime.HostRuntimeContext;
import appbox.store.SysStoreApi;
import appbox.utils.ReflectUtil;
import com.alibaba.ttl.threadpool.TtlExecutors;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class App {

    public static void main(String[] args) {
        //System.setProperty("jna.debug_load", "true");
        //System.setProperty("jna.library.path",dllResourcePath);
        //System.setProperty("jna.platform.library.path",dllResourcePath);

        hackAsyncPool();

        //先判断是否调试服务
        String debugSessionId = null;
        String channelName    = "AppChannel";
        if (args.length > 0) {
            debugSessionId = args[0];
            channelName    = args[0];
        }

        System.out.println("Java AppHost running...");

        //初始化运行时
        var ctx = new HostRuntimeContext(channelName);
        RuntimeContext.init(ctx, (short) 0x1041/*TODO: fix peerId*/);

        //如果调试服务中,预先注入服务实例
        if (debugSessionId != null) {
            ctx.injectDebugService(debugSessionId);
        }
        //Channel并开始阻塞接收
        SysStoreApi.init(ctx.channel);
        ctx.channel.startReceive();

        System.out.println("Java AppHost stopped.");
    }

    private static void hackAsyncPool() {
        //暂在这里Hack CompletableFuture's ASYNC_POOL
        var async_pool = CompletableFuture.completedFuture(true).defaultExecutor();
        //TODO:待尝试ForkJoinPool的AsyncMode
        //Log.debug("ForkJoinPool: Parallelism=" + ForkJoinPool.commonPool().getParallelism()
        //        + " AsyncMode=" + ForkJoinPool.commonPool().getAsyncMode());
        if (async_pool instanceof ExecutorService) {
            async_pool = TtlExecutors.getTtlExecutorService((ExecutorService) async_pool);
        } else {
            async_pool = TtlExecutors.getTtlExecutor(async_pool);
        }

        try {
            ReflectUtil.setFinalStatic(CompletableFuture.class.getDeclaredField("ASYNC_POOL"), async_pool);
        } catch (Exception e) {
            Log.error("Can't find CompletableFuture's ASYNC_POOL field.");
        }
    }

}
