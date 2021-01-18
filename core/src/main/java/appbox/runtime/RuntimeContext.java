package appbox.runtime;

import appbox.logging.Log;
import appbox.model.PermissionModel;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class RuntimeContext {

    private static IRuntimeContext _current;
    private static short           _peerId;

    private RuntimeContext() {}

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

    /** 当前集群节点的标识号 */
    public static short peerId() {
        return _peerId;
    }

    /** 检查当前用户是否具备指定的PermissionModelId的授权 */
    public static boolean hasPermission(long permissionModelId) {
        if (_current == null) return false;

        PermissionModel model = _current.getModel(permissionModelId);
        if (model == null) return false;

        var session = _current.currentSession();
        if (session == null) return false;

        return model.owns(session);
    }

    public static CompletableFuture<Object> invokeAsync(String method, InvokeArgs args) {
        return _current.invokeAsync(method, args);
    }
}
