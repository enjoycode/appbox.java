package appbox.design.handlers;

import appbox.design.DesignHub;
import appbox.runtime.InvokeArg;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class Checkout implements IRequestHandler {
    @Override
    public CompletableFuture<Object> handle(DesignHub hub, List<InvokeArg> args) {
        //TODO:暂简单实现
        return CompletableFuture.completedFuture(true);
    }
}
