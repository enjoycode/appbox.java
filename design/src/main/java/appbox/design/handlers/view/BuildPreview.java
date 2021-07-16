package appbox.design.handlers.view;

import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

/** 生成预览js,仅由主进程调用 */
public final class BuildPreview implements IDesignHandler {
    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        final String path = args.getString();
        return hub.dartLanguageServer.compilePreview(path, false);
    }
}
