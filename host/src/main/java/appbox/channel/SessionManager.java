package appbox.channel;

import appbox.runtime.IService;
import appbox.runtime.ISessionInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 管理用户会话（本机及其他节点?）
 */
public final class SessionManager {

    private static final Map<Long, ISessionInfo> _sessions = new HashMap<>();
    private static final ReadWriteLock           _mapLock  = new ReentrantReadWriteLock();

    private SessionManager() {
    }

    public static ISessionInfo tryGet(long id) {
        var lock = _mapLock.readLock();
        lock.lock();
        var res = _sessions.get(id);
        lock.unlock();
        return res;
    }

    public static void register(ISessionInfo session) {
        var lock = _mapLock.writeLock();
        lock.lock();
        _sessions.put(session.sessionId(), session);
        lock.unlock();
    }

    public static void unregister(long id) {
        var lock = _mapLock.writeLock();
        lock.lock();
        _sessions.remove(id);
        lock.unlock();
    }

}
