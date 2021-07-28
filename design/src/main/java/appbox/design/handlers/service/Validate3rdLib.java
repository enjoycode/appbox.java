package appbox.design.handlers.service;

import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.logging.Log;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

/** 验证上传的第三方类库 */
public final class Validate3rdLib implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        final var uploadPath = args.getString();
        if(uploadPath == null || uploadPath.isEmpty())
            return CompletableFuture.failedFuture(new RuntimeException("Must assign path"));
        if (!uploadPath.endsWith(".jar"))
            return CompletableFuture.failedFuture(new RuntimeException("Only for jar"));

        Log.debug("Validate upload 3rd lib: " + uploadPath);
        return CompletableFuture.completedFuture(true);
    }

}
