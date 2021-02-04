package appbox.channel;

import appbox.channel.messages.StartDebugRequest;
import appbox.channel.messages.StartDebugResponse;
import appbox.design.IDeveloperSession;
import appbox.logging.Log;
import appbox.runtime.RuntimeContext;
import appbox.server.runtime.HostRuntimeContext;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** 管理调试会话及通道 */
public final class DebugSessionManager {

    private static final IHostMessageChannel                hostMessageChannel;
    private static       Map<Long, CompletableFuture<Void>> startings;
    private static       Map<Long, IDeveloperSession>       running;

    static {
        hostMessageChannel = ((HostRuntimeContext) RuntimeContext.current()).channel;
    }

    private DebugSessionManager() {}

    /** 通知主进程准备调试子进程通道并发送调用请求 */
    public static synchronized CompletableFuture<Void> startDebugChannel(IDeveloperSession session
            , String service, byte[] invokeArgs) {
        var future = new CompletableFuture<Void>();

        if (startings == null) {
            startings = new HashMap<>();
            running   = new HashMap<>();
        }
        startings.put(session.sessionId(), future);
        running.put(session.sessionId(), session);
        Log.debug("Starting debug channel: " + Long.toUnsignedString(session.sessionId()));

        var req = new StartDebugRequest(session.sessionId(), service, invokeArgs);
        hostMessageChannel.sendMessage(hostMessageChannel.newMessageId(), req);

        return future;
    }

    /** 收到主进程回复 */
    public static synchronized void onStartResponse(StartDebugResponse response) {
        var future = startings.remove(response.sessionId);
        if (future == null) {
            Log.warn("Can't find pending request: " + Long.toUnsignedString(response.sessionId));
            return;
        }

        if (response.errorCode == 0)
            future.complete(null);
        else
            future.completeExceptionally(new RuntimeException("Can't start debugging"));
    }

    /** 收到主进程正常停止调试的请求 */
    public static synchronized void onStopRequest(long sessionId) {
        var session = running.remove(sessionId);
        if (session == null) {
            Log.warn("Can't find running debug session: " + Long.toUnsignedString(sessionId));
            return;
        }

        session.getDesignHub().debugService().stopDebugger();
    }

}
