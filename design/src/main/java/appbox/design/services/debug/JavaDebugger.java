package appbox.design.services.debug;

import appbox.design.services.debug.variables.Variable;
import appbox.design.services.debug.variables.VariableUtils;
import appbox.logging.Log;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.ListeningConnector;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.*;
import com.sun.jdi.request.EventRequest;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class JavaDebugger {

    private static final ListeningConnector              _listener;
    private static final Map<String, Connector.Argument> _listenerArgs;
    private static final AtomicBoolean                   _listening = new AtomicBoolean(false);

    static {
        var connectors = Bootstrap.virtualMachineManager().listeningConnectors();
        if (connectors == null || connectors.size() == 0) {
            Log.warn("Can't get ListeningConnector for debugging");
            _listener     = null;
            _listenerArgs = null;
        } else {
            _listener     = connectors.get(0);
            _listenerArgs = _listener.defaultArguments();
            //TODO:设置超时
            _listenerArgs.get("port").setValue("5005"); //TODO:改为配置项
            _listenerArgs.get("localAddress").setValue("localhost");
        }
    }

    private final VirtualMachine _vm;
    private final DebugService   _session;
    private final Process        _process;

    private JavaDebugger(VirtualMachine vm, Process process, DebugService session) {
        _vm      = vm;
        _process = process;
        _session = session;

        //请求准备调试的服务类
        var req = vm.eventRequestManager().createClassPrepareRequest();
        req.addClassFilter(session.serviceClassName);
        req.addCountFilter(1);
        req.enable();

        //开始新线程读取事件
        var reader = new Thread(new VMEventsReader(), "receive-vm");
        reader.setDaemon(true);
        reader.start();
    }

    public static synchronized JavaDebugger start(DebugService session) throws IOException, IllegalConnectorArgumentsException {
        if (_listener == null)
            throw new RuntimeException("Debugging not supported");

        if (!_listening.get()) {
            _listening.set(true);
            _listener.startListening(_listenerArgs);
        }

        //先启动调试子进程
        var cmd = List.of("bin/debug",
                Long.toUnsignedString(session.designHub.session.sessionId()));
        var process = new ProcessBuilder().command(cmd).directory(null).inheritIO().start();

        //再接受调试连接
        var vm = _listener.accept(_listenerArgs);
        return new JavaDebugger(vm, process, session);
    }

    public void stop() {
        //TODO:尝试等待目标进程退出至超时，暂强制退出
        _vm.exit(1);
        Log.debug("Stop JavaDebugger");
        //TODO:没有调试会话关闭监听
    }

    public void continue_(long threadId) {
        _vm.resume();
    }

    /** 启用挂起的调试断点 */
    private void enablePendingBreakpoints(ReferenceType serviceType) {
        if (_session.pendingBreakPoints == null || _session.pendingBreakPoints.size() == 0)
            return;

        //TODO:以下不管是否成功，通知前端更新有效断点
        for (var bp : _session.pendingBreakPoints) {
            try {
                var locations = serviceType.locationsOfLine(bp.line);
                if (locations.isEmpty()) {
                    Log.warn("找不到断点位置: " + bp.line);
                } else {
                    for (var line : locations) {
                        var req = _vm.eventRequestManager().createBreakpointRequest(line);
                        req.setSuspendPolicy(EventRequest.SUSPEND_ALL);
                        req.enable();
                    }
                }
            } catch (Exception ex) {
                Log.warn("设置断点错误: " + ex);
            }
        }

        _session.pendingBreakPoints.clear();
    }

    //region ====VmEventsReader====
    class VMEventsReader implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    var nextSet = _vm.eventQueue().remove();
                    for (var event : nextSet) {
                        process(event, nextSet);
                    }
                } catch (VMDisconnectedException __) {
                    Log.info("VM disconnected");
                    return;
                } catch (Exception e) {
                    Log.error(e.getMessage());
                    return;
                }
            }
        }

        private void process(Event event, EventSet eventSet) {
            Log.debug("Received " + event.toString() + " from VM");
            if (event instanceof VMStartEvent) {
                _session.designHub.session.sendEvent(DebugStartEvent.Default);
                Log.debug("VM started");
                eventSet.resume();
            } else if (event instanceof ClassPrepareEvent) {
                var prepare = (ClassPrepareEvent) event;
                var type    = prepare.referenceType();
                Log.debug("ClassPrepared for class " + type.name());
                enablePendingBreakpoints(type);
                eventSet.resume();
            } else if (event instanceof BreakpointEvent) {
                var breakpointEvent = (BreakpointEvent) event;
                Log.debug("Stopped at break point: " + breakpointEvent.location().lineNumber());

                //测试读取变量
                List<Variable> variables = null;
                try {
                    variables = VariableUtils.listLocalVariables(breakpointEvent.thread().frame(0));
                } catch (Exception ex) {
                    Log.warn("读取变量错误: " + ex);
                }

                _session.designHub.session.sendEvent(new DebugPauseEvent(
                        breakpointEvent.thread().uniqueID(),
                        breakpointEvent.location().lineNumber(),
                        variables));
            } else if (event instanceof StepEvent) {
                var ste = (StepEvent) event;
                //var evt = new StoppedEventBody();
                //evt.reason = "step";
                //evt.threadId = b.thread().uniqueID();
                //evt.allThreadsStopped = b.request().suspendPolicy() == EventRequest.SUSPEND_ALL;
                //client.stopped(evt);
                //// Disable event so we can create new step events
                //event.request().disable();
            } else if (event instanceof VMDeathEvent) {
                //client.exited(new ExitedEventBody());
            } else if (event instanceof VMDisconnectEvent) {
                _session.designHub.session.sendEvent(DebugStopEvent.Default);
            }
        }
    }
    //endregion
}
