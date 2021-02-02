package appbox.design.services.debug;

import appbox.design.DesignHub;
import appbox.logging.Log;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/** 一个DesignHub对应一个实例 */
public final class DebugService {

    private final AtomicBoolean _debugging = new AtomicBoolean(false);
    public final  DesignHub     designHub;
    protected     String        serviceClassName;
    private       JavaDebugger  _debugger;

    public DebugService(DesignHub hub) {
        designHub = hub;
    }

    /**
     * 1. 通知主进程准备好调试子进程的消息通道
     * 2. 启动Java调试器及调试子进程
     */
    public CompletableFuture<Void> startDebugger(String appName, String serviceName, String methodName
            , byte[] invokeArgs, String breakPoints) {
        if (_debugging.get())
            throw new RuntimeException("Already in debugging");

        serviceClassName = serviceName;
        final var servicePath = String.format("%s.%s.%s", appName, serviceName, methodName);

        return designHub.session.startDebugChannel(servicePath, invokeArgs).thenAccept(r -> {
            _debugging.set(true);
            try {
                _debugger = JavaDebugger.start(this);
                Log.debug("Start debugging: " + servicePath);
            } catch (Exception ex) {
                _debugging.set(false);
                throw new RuntimeException("Can't start java debugger");
            }
        });
    }

    public void stopDebugger() {
        if (!_debugging.get()) {
            Log.warn("Hasn't start JavaDebugger");
            return;
        }
        if (_debugger == null) {
            Log.warn("Can't get JavaDebugger");
            return;
        }

        _debugger.stop();
        _debugger = null;
        _debugging.set(false);
    }

}
