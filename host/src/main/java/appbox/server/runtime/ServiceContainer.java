package appbox.server.runtime;

import appbox.design.services.DesignService;
import appbox.logging.Log;
import appbox.runtime.IService;
import appbox.server.services.SystemService;
import appbox.server.services.TestService;
import appbox.store.ModelStore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 服务端服务实例容器
 */
public final class ServiceContainer {

    private final Map<CharSequence, IService> _services = new HashMap<>();
    private final ReadWriteLock               _mapLock  = new ReentrantReadWriteLock();

    public ServiceContainer() {
        //注册系统服务
        registerService("sys.System", new SystemService());
        registerService("sys.DesignService", new DesignService());
        registerService("sys.OrderService", new TestService()); //TODO:测试待移除
    }

    /**
     * 注册服务
     * @param service eg: "sys.System"
     */
    private void registerService(CharSequence service, IService instance) {
        var lock = _mapLock.writeLock();
        lock.lock();
        //TODO:判断是否存在
        _services.putIfAbsent(service, instance);
        lock.unlock();
    }

    /**
     * 尝试根据名称获取服务实例，不存在或加载异常返回Null
     */
    public IService tryGet(CharSequence service) {
        _mapLock.readLock().lock();
        var instance = _services.get(service);
        _mapLock.readLock().unlock();
        if (instance != null)
            return instance;

        //从模型存储加载,暂异步转同步
        _mapLock.writeLock().lock();
        try {
            var serviceFullName = service.toString();
            var firstDotIndex   = serviceFullName.indexOf('.');
            var serviceName     = serviceFullName.substring(firstDotIndex + 1);
            var asmData         = ModelStore.loadServiceAssemblyAsync(serviceFullName).get(5, TimeUnit.SECONDS);
            if (asmData == null)
                throw new RuntimeException("Can't load assembly from ModelStore");

            var serviceClassLoader = new ServiceClassLoader();
            var clazz              = serviceClassLoader.loadServiceClass(serviceName, asmData);
            instance = (IService) clazz.getDeclaredConstructor().newInstance();
            _services.put(service, instance);
        } catch (Exception ex) {
            Log.warn("Load service assembly[" + service + "] error:" + ex.getMessage());
        } finally {
            _mapLock.writeLock().unlock();
        }

        return instance;
    }

}
