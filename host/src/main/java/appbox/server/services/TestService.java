package appbox.server.services;

import appbox.logging.Log;
import appbox.runtime.IService;
import appbox.runtime.InvokeArgs;
import appbox.runtime.RuntimeContext;

import java.util.concurrent.CompletableFuture;

/**
 * 仅用于测试
 */
public final class TestService implements IService {
    @Override
    public CompletableFuture<Object> invokeAsync(CharSequence method, InvokeArgs args) {
        Log.debug("调用测试服务时的会话：" + RuntimeContext.current().currentSession().name());

        return CompletableFuture.completedFuture("Hello Future! 你好，未来!");
        //long modelId = 0x9E9AA8F702000004L;
        //var  req     = new KVGetModelRequest(modelId);
        //return SysStoreApi.execKVGetAsync(req).thenApply(r -> {
        //    return r.result;
        //});
    }
}
