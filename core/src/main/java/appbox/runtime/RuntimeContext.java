package appbox.runtime;

import appbox.logging.Log;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class RuntimeContext {

    private static IRuntimeContext _current;
    private static short           _peerId;

    private RuntimeContext() {
    }

    public static void init(IRuntimeContext context, short peerId) {
        if (_current != null) {
            Log.warn("RuntimeContext has value");
            return;
        }

        _current = context;
        _peerId  = peerId;
    }

    public static IRuntimeContext current() {
        return _current;
    }

    public static short peerId() {
        return _peerId;
    }

    public static CompletableFuture<Object> invokeAsync(String method, InvokeArgs args) {
        return _current.invokeAsync(method, args);
    }
}
