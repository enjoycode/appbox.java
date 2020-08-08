package appbox.core.runtime;

import appbox.core.logging.Log;

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

    public static CompletableFuture<Object> invokeAsync(CharSequence method, List<InvokeArg> args) {
        return _current.invokeAsync(method, args);
    }
}
