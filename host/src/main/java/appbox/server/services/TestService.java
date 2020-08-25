package appbox.server.services;

import appbox.channel.messages.KVGetModelRequest;
import appbox.runtime.IService;
import appbox.runtime.InvokeArg;
import appbox.store.SysStoreApi;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 仅用于测试
 */
public final class TestService implements IService {
    @Override
    public CompletableFuture<Object> invokeAsync(CharSequence method, List<InvokeArg> args) {
        long modelId = 0x9E9AA8F702000004L;
        var  req     = new KVGetModelRequest(modelId);
        return SysStoreApi.execKVGetAsync(req).thenApply(r -> {
            return r.result;
        });
    }
}
