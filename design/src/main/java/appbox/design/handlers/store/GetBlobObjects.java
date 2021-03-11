package appbox.design.handlers.store;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.runtime.InvokeArgs;
import appbox.store.BlobStore;

import java.util.concurrent.CompletableFuture;

public final class GetBlobObjects implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var storeName = args.getString();
        var path      = args.getString();

        return BlobStore.get(storeName).listAsync(path).thenApply(JsonResult::new);
    }

}
