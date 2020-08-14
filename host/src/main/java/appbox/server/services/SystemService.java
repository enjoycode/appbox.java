package appbox.server.services;

import appbox.core.runtime.IService;
import appbox.core.runtime.InvokeArg;
import appbox.store.StoreInitiator;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class SystemService implements IService {

    @Override
    public CompletableFuture<Object> invokeAsync(CharSequence method, List<InvokeArg> args) {
        if (method.equals("InitStore")) {
            return StoreInitiator.initAsync().thenApply(ok -> ok);
        } else {
            var ex = new NoSuchMethodException("SystemService can't find method: " + method.toString());
            return CompletableFuture.failedFuture(ex);
        }
    }
}
