package appbox.store;

import appbox.core.model.ApplicationModel;
import appbox.server.channel.IMessageChannel;
import appbox.server.channel.messages.IMessage;
import appbox.server.channel.messages.NewAppRequire;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统存储的Api，通过消息通道与主进程的存储引擎交互
 */
public final class SysStoreApi {
    private static       IMessageChannel                                         _channel;
    private static final ConcurrentHashMap<Integer, CompletableFuture<IMessage>> _pendings = new ConcurrentHashMap<>();

    private SysStoreApi() {
    }

    public static void init(IMessageChannel channel) {
        _channel = channel;
    }

    /**
     * 收到系统存储的响应
     */
    public static void onResponse(int reqId, IMessage response) {
        var task = _pendings.remove(reqId);
        if (task == null) {
            //TODO: log it
        } else {
            task.complete(response);
        }
    }

    public static CompletableFuture<Byte> createApplicationAsync(ApplicationModel app) {
        var msgId = _channel.newMessageId();
        var task  = new CompletableFuture<IMessage>();
        //加入等待列表 TODO:检测已存在
        _pendings.put(msgId, task);
        var req = new NewAppRequire(app);
        try {
            _channel.sendMessage(msgId, req);
        } catch (Exception e) {
            //TODO:返回异步异常
        }

        return task.thenApply(m -> {
            return (byte) 1;
        });
    }
}
