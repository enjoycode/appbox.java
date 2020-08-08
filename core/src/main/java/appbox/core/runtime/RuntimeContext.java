package appbox.core.runtime;

import appbox.core.logging.Log;

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
}
