package appbox.design.handlers.view;

import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

/** 生成并发布Web应用 */
public final class BuildWebApp implements IDesignHandler {
    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        final String  appName        = args.getString();
        final boolean isHtmlRenderer = args.getBool();

        return hub.dartLanguageServer.buildWebApp(appName, isHtmlRenderer, false);
    }
}
