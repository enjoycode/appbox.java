package appbox.server.runtime;

import appbox.design.services.DesignService;
import appbox.logging.Log;
import appbox.runtime.IService;
import appbox.server.services.AdminService;
import appbox.server.services.SystemService;
import appbox.server.services.TestService;
import appbox.store.ModelStore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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
        registerService("sys.AdminService", new AdminService());
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
     * 尝试根据名称获取已加载的服务实例
     */
    public IService tryGet(CharSequence service) {
        _mapLock.readLock().lock();
        var instance = _services.get(service);
        _mapLock.readLock().unlock();
        return instance;
    }

    /** 尝试从ModelStore加载服务实例 */
    public CompletableFuture<IService> tryLoadAsync(CharSequence service) {
        var serviceFullName = service.toString();
        var firstDotIndex   = serviceFullName.indexOf('.');
        var serviceName     = serviceFullName.substring(firstDotIndex + 1);

        //TODO:暂并发时会多余加载，待修改
        return ModelStore.loadServiceAssemblyAsync(serviceFullName).handle((r, ex) -> {
            if (ex != null || r == null) {
                String error = ex == null ? "Not exists" : ex.toString();
                Log.error("Load service assembly[" + service + "] error: " + error);
                return null;
            }

            _mapLock.writeLock().lock();
            var instance = _services.get(service);
            if (instance != null) {
                _mapLock.writeLock().unlock();
                return instance;
            }

            //创建服务实例
            try {
                var serviceClassLoader = new ServiceClassLoader();
                var clazz              = serviceClassLoader.loadServiceClass(serviceName, r);
                var obj                = (IService) clazz.getDeclaredConstructor().newInstance();
                _services.put(service, obj);
                return obj;
            } catch (Exception e) {
                Log.error("Create service instance[" + service + "] error:" + e.getMessage());
                return null;
            } finally {
                _mapLock.writeLock().unlock();
            }
        });
    }

    public void tryRemove(CharSequence service) {
        _mapLock.writeLock().lock();
        _services.remove(service);
        _mapLock.writeLock().unlock();
    }

}
