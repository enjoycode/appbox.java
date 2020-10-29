package appbox.server.runtime;

import appbox.logging.Log;
import appbox.runtime.IRuntimeContext;
import appbox.runtime.ISessionInfo;
import appbox.runtime.InvokeArg;
import appbox.utils.ReflectUtil;
import com.alibaba.ttl.TransmittableThreadLocal;
import com.alibaba.ttl.threadpool.TtlExecutors;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public final class HostRuntimeContext implements IRuntimeContext {

    private final        ServiceContainer                       _services   = new ServiceContainer();
    private static final TransmittableThreadLocal<ISessionInfo> _sessionTTL = new TransmittableThreadLocal<>();

    static {
        //暂在这里Hack CompletableFuture's ASYNC_POOL
        var async_pool = CompletableFuture.completedFuture(true).defaultExecutor();
        if (async_pool instanceof ExecutorService) {
            async_pool = TtlExecutors.getTtlExecutorService((ExecutorService) async_pool);
        } else {
            async_pool = TtlExecutors.getTtlExecutor(async_pool);
        }

        try {
            ReflectUtil.setFinalStatic(CompletableFuture.class.getDeclaredField("ASYNC_POOL"), async_pool);
        } catch (Exception e) {
            Log.error("Can't find CompletableFuture's ASYNC_POOL field.");
        }
    }

    /**
     * 仅用于消息分发器调用服务前设置以及系统存储收到请求响应时设置
     */
    @Override
    public void setCurrentSession(@Nullable ISessionInfo session) {
        if (session == null) {
            _sessionTTL.remove();
        } else {
            _sessionTTL.set(session);
        }
    }

    @Override
    public ISessionInfo currentSession() {
        return _sessionTTL.get();
    }

    @Override
    public CompletableFuture<Object> invokeAsync(String method, List<InvokeArg> args) {
        //从服务容器内找到服务实例
        var methodDotIndex = method.lastIndexOf('.');
        var servicePath    = method.subSequence(0, methodDotIndex);
        var service        = _services.tryGet(servicePath);
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
