package appbox.server.runtime;

import appbox.core.runtime.IService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 服务端服务实例容器
 */
public final class ServiceContainer {

    private final Map<CharSequence, IService> _services = new HashMap<>();
    private final ReadWriteLock _mapLock = new ReentrantReadWriteLock();

}
