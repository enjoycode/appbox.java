package appbox.design.handlers.service;

import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

public final class ContinueDebug implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var threadId = args.getLong();
        hub.debugService().resume(threadId);
        return CompletableFuture.completedFuture(null);
    }

}
