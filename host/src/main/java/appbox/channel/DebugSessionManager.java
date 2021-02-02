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

    private DebugSessionManager() {}

    public static void init(IHostMessageChannel channel) {
        hostMessageChannel = channel;
    }

    public static synchronized CompletableFuture<Void> startDebugChannel(IDeveloperSession session
            , String service, byte[] invokeArgs) {
        var future = new CompletableFuture<Void>();

        if (startings == null)
            startings = new HashMap<>();
        startings.put(session.sessionId(), future);

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

        if (response.ok)
            future.completeAsync(() -> null);
        else
            CompletableFuture.runAsync(() -> future.completeExceptionally(new RuntimeException("Can't start debugging")));
    }

}
