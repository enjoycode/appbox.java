package appbox.store;

import appbox.core.logging.Log;
import appbox.core.model.ApplicationModel;
import appbox.server.channel.IMessageChannel;
import appbox.server.channel.messages.IMessage;
import appbox.server.channel.messages.NewAppRequire;
import appbox.server.channel.messages.NewAppResponse;

import java.io.IOException;
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
        Log.debug("收到存储引擎响应消息: " + reqId);
        var task = _pendings.remove(reqId);
        if (task == null) {
            Log.warn("收到存储引擎响应时找不到相应的请求:" + reqId);
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
            Log.warn("发送新建应用请求消息错误: " + e.getMessage());
            //返回异步异常
            return CompletableFuture.failedFuture(new IOException("Can't send message to channel."));
        }

        //TODO:处理存储引擎异常
        return task.thenApply(m -> {
            return ((NewAppResponse) m).appId;
        });
    }
}
