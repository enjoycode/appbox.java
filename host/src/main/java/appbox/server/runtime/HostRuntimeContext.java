package appbox.server.runtime;

import appbox.logging.Log;
import appbox.model.ApplicationModel;
import appbox.model.ModelBase;
import appbox.runtime.IPasswordHasher;
import appbox.runtime.IRuntimeContext;
import appbox.runtime.ISessionInfo;
import appbox.runtime.InvokeArgs;
import appbox.server.security.PasswordHasher;
import appbox.store.ModelStore;
import appbox.utils.ReflectUtil;
import com.alibaba.ttl.TransmittableThreadLocal;
import com.alibaba.ttl.threadpool.TtlExecutors;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public final class HostRuntimeContext implements IRuntimeContext {

    private final        ServiceContainer                       _services   = new ServiceContainer();
    private static final TransmittableThreadLocal<ISessionInfo> _sessionTTL = new TransmittableThreadLocal<>();

    private final IPasswordHasher _passwordHasher = new PasswordHasher();

    private final ArrayList<ApplicationModel> apps   = new ArrayList<>(); //TODO:use RWLock
    private final HashMap<Long, ModelBase>    models = new HashMap<>(100); //TODO:usr LRUCache

    static {
        //暂在这里Hack CompletableFuture's ASYNC_POOL
        var async_pool = CompletableFuture.completedFuture(true).defaultExecutor();
        //TODO:待尝试ForkJoinPool的AsyncMode
        //Log.debug("ForkJoinPool: Parallelism=" + ForkJoinPool.commonPool().getParallelism()
        //        + " AsyncMode=" + ForkJoinPool.commonPool().getAsyncMode());
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
    public IPasswordHasher passwordHasher() {
        return _passwordHasher;
    }

    @Override
    public CompletableFuture<Object> invokeAsync(String method, InvokeArgs args) {
        var methodDotIndex = method.lastIndexOf('.');
        var servicePath    = method.subSequence(0, methodDotIndex);
        var methodName     = method.subSequence(methodDotIndex + 1, method.length());
        //从服务容器内找到服务实例
        var service = _services.tryGet(servicePath);
        if (service != null) { //已加载服务实例
            return service.invokeAsync(methodName, args);
        }
        //不存在则加载
        return _services.tryLoadAsync(servicePath)
                .thenCompose(instance -> {
                    if (instance == null)
                        return CompletableFuture.failedFuture(new ClassNotFoundException("Can't find service"));
                    return instance.invokeAsync(methodName, args);
                });
    }

    //region ====ModelContainer====

    /** 仅用于StoreInitiator */
    public void injectApplication(ApplicationModel appModel) {
        apps.add(appModel);
    }

    /** 仅用于StoreInitiator */
    public void injectModel(ModelBase model) {
        model.acceptChanges();
        models.putIfAbsent(model.id(), model);
    }

    @Override
    public ApplicationModel getApplicationModel(int appId) {
        for (var app : apps) {
            if (app.id() == appId) {
                return app;
            }
        }

        try {
            var appModel = ModelStore.loadApplicationAsync(appId).get();
            if (appModel == null) {
                Log.warn("Can't load application model:" + appId);
                return null;
            }

            synchronized (apps) {
                boolean exists = false;
                for (var app : apps) {
                    if (app.id() == appId) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    apps.add(appModel);
                }
            }

            return appModel;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ModelBase> T getModel(long modelId) {
        var model = models.get(modelId);
        if (model != null) {
            return (T) model;
        }

        try {
            model = ModelStore.loadModelAsync(modelId).get();
            if (model == null) {
                Log.warn("Can't load model: " + modelId);
                return null;
            }

            synchronized (models) {
                models.putIfAbsent(modelId, model);
            }
            return (T) model;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    @Override
    public void invalidModelsCache(String[] services, long[] others, boolean byPublish) {
        if (others != null) {
            for (var other : others) {
                models.remove(other);
            }
        }

        if (services != null) {
            for (var service : services) {
                _services.tryRemove(service);
            }
        }

        //if (byPublish) {
        //TODO:***** 最后通知整个集群
        //}
    }
    //endregion

}
