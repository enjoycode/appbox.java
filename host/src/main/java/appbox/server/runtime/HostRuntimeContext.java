package appbox.server.runtime;

import appbox.core.logging.Log;
import appbox.core.runtime.IRuntimeContext;
import appbox.core.runtime.InvokeArg;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class HostRuntimeContext implements IRuntimeContext {

    private final ServiceContainer _services = new ServiceContainer();

    @Override
    public CompletableFuture<Object> invokeAsync(String method, List<InvokeArg> args) {
        //从服务容器内找到服务实例
        var methodDotIndex = method.lastIndexOf('.');
        var servicePath = method.subSequence(0, methodDotIndex);
        var service = _services.tryGet(servicePath);
        if (service == null) {
            var error = "Can't find service: " + servicePath.toString();
            Log.warn(error);
            return CompletableFuture.failedFuture(new ClassNotFoundException(error));
        }
        //调用服务实例的方法
        var methodName = method.subSequence(methodDotIndex + 1, method.length());
        return service.invokeAsync(methodName, args);
    }
}
