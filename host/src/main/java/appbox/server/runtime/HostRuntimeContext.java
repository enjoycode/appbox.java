package appbox.server.runtime;

import appbox.channel.IHostMessageChannel;
import appbox.channel.SharedMemoryChannel;
import appbox.logging.Log;
import appbox.model.ApplicationModel;
import appbox.model.ModelBase;
import appbox.runtime.IPasswordHasher;
import appbox.runtime.IRuntimeContext;
import appbox.runtime.IUserSession;
import appbox.runtime.InvokeArgs;
import appbox.server.security.PasswordHasher;
import appbox.store.ModelStore;
import com.alibaba.ttl.TransmittableThreadLocal;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public final class HostRuntimeContext implements IRuntimeContext {

    private static final TransmittableThreadLocal<IUserSession> _sessionTTL = new TransmittableThreadLocal<>();

    private final ServiceContainer    _services       = new ServiceContainer();
    private final IPasswordHasher     _passwordHasher = new PasswordHasher();
    public final  IHostMessageChannel channel; //与主进程连接的通道

    private final ArrayList<ApplicationModel> apps   = new ArrayList<>(); //TODO:use RWLock
    private final HashMap<Long, ModelBase>    models = new HashMap<>(100); //TODO:usr LRUCache

    public HostRuntimeContext(String channelName) {
        channel = new SharedMemoryChannel(channelName);
    }

    /** 仅用于消息分发器调用服务前设置以及系统存储收到请求响应时设置 */
    @Override
    public void setCurrentSession(@Nullable IUserSession session) {
        if (session == null) {
            _sessionTTL.remove();
        } else {
            _sessionTTL.set(session);
        }
    }

    @Override
    public IUserSession currentSession() {
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
        return _services.tryLoadAsync(servicePath).thenCompose(instance -> {
            if (instance == null) {
                var error = new ClassNotFoundException("Can't find service: " + servicePath);
                return CompletableFuture.failedFuture(error);
            }
            return instance.invokeAsync(methodName, args);
        });
    }

    //region ====ModelContainer====

    public void injectDebugService(String debugSessionId) {
        _services.injectDebugService(debugSessionId);
    }

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
