package appbox.server.runtime;

import appbox.design.services.DesignService;
import appbox.design.utils.PathUtil;
import appbox.logging.Log;
import appbox.runtime.IService;
import appbox.server.services.AdminService;
import appbox.server.services.SystemService;
import appbox.server.services.TestService;
import appbox.store.ModelStore;

import java.nio.file.Files;
import java.nio.file.Path;
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

    /** 预先注入调试目标服务,防止从存储加载 */
    protected void injectDebugService(String debugSessionId) {
        var dbgPath = PathUtil.getDebugPath(debugSessionId);
        if (!Files.exists(dbgPath))
            throw new RuntimeException("Debug path not exists");

        byte[] pkgData = null;
        try {
            var files           = Files.list(dbgPath).toArray(Path[]::new);
            var filePath        = files[0];
            var fileName        = filePath.toFile().getName();
            var serviceFullName = fileName.substring(0, fileName.length() - 4);
            var serviceName     = getServiceName(serviceFullName);
            pkgData = Files.readAllBytes(filePath);
            var serviceClassLoader = new ServiceClassLoader();
            var clazz              = serviceClassLoader.loadServiceClass(serviceName, pkgData);
            var obj                = (IService) clazz.getDeclaredConstructor().newInstance();
            registerService(serviceFullName, obj);
            Log.debug("Inject debug service: " + serviceFullName);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /** 尝试根据名称获取已加载的服务实例 */
    public IService tryGet(CharSequence service) {
        _mapLock.readLock().lock();
        var instance = _services.get(service);
        _mapLock.readLock().unlock();
        return instance;
    }

    /** 尝试从ModelStore加载服务实例 */
    public CompletableFuture<IService> tryLoadAsync(CharSequence service) {
        var serviceFullName = service.toString();
        var serviceName     = getServiceName(serviceFullName);

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
                var obj = loadServiceInstance(serviceName, r);
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

    /**
     * 从服务全路径中获取名称
     * @param serviceFullName eg: sys.OrderService
     * @return eg: OrderService
     */
    private static String getServiceName(String serviceFullName) {
        var firstDotIndex = serviceFullName.indexOf('.');
        return serviceFullName.substring(firstDotIndex + 1);
    }

    private static IService loadServiceInstance(String serviceName, byte[] data) throws Exception {
        var serviceClassLoader = new ServiceClassLoader();
        var clazz              = serviceClassLoader.loadServiceClass(serviceName, data);
        return (IService) clazz.getDeclaredConstructor().newInstance();
    }

    public void tryRemove(CharSequence service) {
        _mapLock.writeLock().lock();
        _services.remove(service);
        _mapLock.writeLock().unlock();
    }

}
