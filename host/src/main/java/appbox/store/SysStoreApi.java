package appbox.store;

import appbox.core.logging.Log;
import appbox.core.model.ApplicationModel;
import appbox.server.channel.IMessageChannel;
import appbox.server.channel.messages.*;

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
        //Log.debug("收到存储引擎响应消息: " + reqId);
        var task = _pendings.remove(reqId);
        if (task == null) {
            Log.warn("收到存储引擎响应时找不到相应的请求:" + reqId);
        } else {
            task.complete(response);
        }
    }

    /**
     * 收到系统存储的响应，但是反序列化失败
     */
    public static void onResponseDeserializeError(int reqId) {
        var task = _pendings.remove(reqId);
        if (task == null) {
            Log.warn("收到存储引擎响应时找不到相应的请求:" + reqId);
        } else {
            task.completeExceptionally(new IOException("反序列化存储响应失败"));
        }
    }

    /**
     * 根据请求消息新建异步任务加入挂起的字典表，并且序列化发送请求消息
     * @return 如果序列化或发送失败返回null
     */
    private static CompletableFuture<IMessage> makeTaskAndSendRequire(IMessage require) {
        var msgId = _channel.newMessageId();
        var task  = new CompletableFuture<IMessage>();
        //加入等待列表 TODO:检测已存在
        _pendings.put(msgId, task);
        try {
            _channel.sendMessage(msgId, require);
        } catch (Exception e) {
            Log.warn("发送请求消息[" + require.getClass().getName() + "]错误: " + e.getMessage());
            return null;
        }
        return task;
    }

    //region ====Transaction====
    public static CompletableFuture<KVBeginTxnResponse> beginTxnAsync(/*TODO:isoLevel*/) {
        var task = makeTaskAndSendRequire(new KVBeginTxnRequire());
        if (task == null) {
            //返回异步异常
            return CompletableFuture.failedFuture(new IOException("Can't send message to channel."));
        }

        //TODO:处理存储引擎异常
        return task.thenApply(m -> (KVBeginTxnResponse) m);
    }

    public static CompletableFuture<KVCommandResponse> endTxnAsync(KVEndTxnRequire req) {
        var task = makeTaskAndSendRequire(req);
        if (task == null) {
            //返回异步异常
            return CompletableFuture.failedFuture(new IOException("Can't send message to channel."));
        }

        //TODO:处理存储引擎异常
        return task.thenApply(m -> (KVCommandResponse) m);
    }
    //endregion

    //region ====Meta====
    protected static CompletableFuture<Byte> createApplicationAsync(ApplicationModel app) {
        var task = makeTaskAndSendRequire(new NewAppRequire(app));
        if (task == null) {
            //返回异步异常
            return CompletableFuture.failedFuture(new IOException("Can't send message to channel."));
        }

        //TODO:处理存储引擎异常
        return task.thenApply(m -> ((NewAppResponse) m).appId);
    }
    //endregion

    //region ====KVCommands====
    public static CompletableFuture<KVCommandResponse> execKVInsertAsync(KVInsertRequire cmd) {
        var task = makeTaskAndSendRequire(cmd);
        if (task == null) {
            //返回异步异常
            return CompletableFuture.failedFuture(new IOException("Can't send message to channel."));
        }

        //TODO:处理存储引擎异常
        return task.thenApply(m -> (KVCommandResponse) m);
    }
    //endregion
}
