package appbox.channel;

import appbox.channel.messages.StartDebugRequest;
import appbox.channel.messages.StartDebugResponse;
import appbox.design.IDeveloperSession;
import appbox.logging.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** 管理调试会话及通道 */
public final class DebugSessionManager {

    private static IHostMessageChannel                hostMessageChannel;
    private static Map<Long, CompletableFuture<Void>> startings;
    private static Map<Long, IDeveloperSession>       running;

    private DebugSessionManager() {}

    public static void init(IHostMessageChannel channel) {
        hostMessageChannel = channel;
    }

    public static synchronized CompletableFuture<Void> startDebugChannel(IDeveloperSession session
            , String service, byte[] invokeArgs) {
        var future = new CompletableFuture<Void>();

        if (startings == null) {
            startings = new HashMap<>();
            running   = new HashMap<>();
        }
        startings.put(session.sessionId(), future);
        running.put(session.sessionId(), session);

        var req = new StartDebugRequest(session.sessionId(), service, invokeArgs);
        hostMessageChannel.sendMessage(hostMessageChannel.newMessageId(), req);

        return future;
    }

    public static synchronized void onStartResponse(StartDebugResponse response) {
        var future = startings.remove(response.sessionId);
        if (future == null) {
            Log.warn("Can't find pending request");
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
            Log.warn("Can't find running debug session");
            return;
        }

        session.getDesignHub().debugService().stopDebugger();
    }

}
