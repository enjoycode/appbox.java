package appbox.server.runtime;

import appbox.core.runtime.IRuntimeContext;
import appbox.core.runtime.InvokeArg;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class HostRuntimeContext implements IRuntimeContext {

    private final ServiceContainer _services = new ServiceContainer();

    @Override
    public CompletableFuture<Object> invokeAsync(CharSequence method, List<InvokeArg> args) {
        //TODO:从服务容器内找到服务实例进行调用
        return CompletableFuture.completedFuture("Hello World! Hello Future!");
    }
}
