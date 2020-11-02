package appbox.design.handlers;

import appbox.design.DesignHub;
import appbox.runtime.InvokeArg;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class Checkout implements IRequestHandler {
    /**
     * 签出编辑模型
     * @return true表示签出时模型已被其他人改变
     */
    @Override
    public CompletableFuture<Object> handle(DesignHub hub, List<InvokeArg> args) {
        //TODO:暂简单实现
        return CompletableFuture.completedFuture(false);
    }
}
