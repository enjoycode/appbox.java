package appbox.design.services.debug;

import appbox.design.DesignHub;

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

    public void startDebugger(String appName, String serviceName, String methodName
            , byte[] invokeArgs, String breakPoints) {
        if (_debugging.get())
            throw new RuntimeException("Already in debugging");
        _debugging.set(true);

        serviceClassName = serviceName;
        try {
            _debugger = JavaDebugger.start(this);
        } catch (Exception ex) {
            _debugging.set(false);
            throw new RuntimeException("Can't start java debugger");
        }
    }

}
