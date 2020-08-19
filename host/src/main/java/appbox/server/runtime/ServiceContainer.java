package appbox.server.runtime;

import appbox.runtime.IService;
import appbox.server.services.SystemService;

import java.util.HashMap;
import java.util.Map;
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
    }

    /**
     * 注册服务
     *
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
     * 尝试根据名称获取服务实例，不存在返回Null
     */
    public IService tryGet(CharSequence service) {
        var lock = _mapLock.readLock();
        lock.lock();
        var instance = _services.get(service);
        lock.unlock();
        //TODO:从模型存储加载
        return instance;
    }

}
