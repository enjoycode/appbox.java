package appbox.store;

import appbox.channel.IMessage;
import appbox.logging.Log;
import appbox.model.ApplicationModel;
import appbox.channel.IMessageChannel;
import appbox.channel.messages.*;
import appbox.runtime.RuntimeContext;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统存储的Api，通过消息通道与主进程的存储引擎交互
 * 注意: 所有响应不抛出异常，调用者根据响应的ErrorCode作相应的处理
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
        //注意处理当前会话信息，发送前获取，收到回复后恢复
        var session = RuntimeContext.current().currentSession();
        //加入等待列表 TODO:检测已存在，另考虑将会话信息一并放入字典表，在onResponse恢复会话
        _pendings.put(msgId, task);
        try {
            _channel.sendMessage(msgId, require);
        } catch (Exception e) {
            Log.warn("发送请求消息[" + require.getClass().getName() + "]错误: " + e.getMessage());
            return null;
        }
        return task.thenApply(m -> {
            //先恢复会话信息
            RuntimeContext.current().setCurrentSession(session);
            //再返回响应消息
            return m;
        });
    }

    private static <T extends StoreResponse> CompletableFuture<T> makeSendRequireError(T response) {
        response.errorCode = 2 << 24; //TODO:fix error code
        return CompletableFuture.completedFuture(response);
    }

    //region ====Transaction====
    public static CompletableFuture<KVBeginTxnResponse> beginTxnAsync(/*TODO:isoLevel*/) {
        var task = makeTaskAndSendRequire(new KVBeginTxnRequire());
        if (task == null) {
            return makeSendRequireError(new KVBeginTxnResponse());
        }
        return task.thenApply(m -> (KVBeginTxnResponse) m);
    }

    public static CompletableFuture<KVCommandResponse> commitTxnAsync(KVTxnId txnId) {
        var req = new KVEndTxnRequire();
        req.txnId.copyFrom(txnId);
        req.action = 0;
        return endTxnAsync(req);
    }

    public static CompletableFuture<KVCommandResponse> rollbackTxnAsync(KVTxnId txnId) {
        var req = new KVEndTxnRequire();
        req.txnId.copyFrom(txnId);
        req.action = 1;
        return endTxnAsync(req);
    }

    private static CompletableFuture<KVCommandResponse> endTxnAsync(KVEndTxnRequire req) {
        var task = makeTaskAndSendRequire(req);
        if (task == null) {
            return makeSendRequireError(new KVCommandResponse());
        }
        return task.thenApply(m -> (KVCommandResponse) m);
    }
    //endregion

    //region ====Meta====
    protected static CompletableFuture<MetaNewAppResponse> metaNewAppAsync(ApplicationModel app) {
        var task = makeTaskAndSendRequire(new MetaNewAppRequire(app));
        if (task == null) {
            return makeSendRequireError(new MetaNewAppResponse());
        }
        return task.thenApply(m -> ((MetaNewAppResponse) m));
    }

    protected static CompletableFuture<MetaGenModelIdResponse> metaGenModelIdAsync(int appId, boolean devLayer) {
        var req = new MetaGenModelIdRequire();
        req.appId    = appId;
        req.devLayer = devLayer;

        var task = makeTaskAndSendRequire(req);
        if (task == null) {
            return makeSendRequireError(new MetaGenModelIdResponse());
        }
        return task.thenApply(m -> ((MetaGenModelIdResponse) m));
    }

    protected static CompletableFuture<MetaGenPartitionResponse> metaGenPartitionAsync(PartitionInfo info, KVTxnId txnId) {
        var req  = new MetaGenPartitionRequest(info, txnId);
        var task = makeTaskAndSendRequire(req);
        if (task == null) {
            return makeSendRequireError(new MetaGenPartitionResponse());
        }
        return task.thenApply(m -> ((MetaGenPartitionResponse) m));
    }
    //endregion

    //region ====KVCommands====
    public static CompletableFuture<KVCommandResponse> execKVInsertAsync(KVInsertRequire cmd) {
        var task = makeTaskAndSendRequire(cmd);
        if (task == null) {
            return makeSendRequireError(new KVCommandResponse());
        }
        return task.thenApply(m -> (KVCommandResponse) m);
    }

    public static CompletableFuture<KVCommandResponse> execKVDeleteAsync(KVDeleteRequire req) {
        var task = makeTaskAndSendRequire(req);
        if (task == null) {
            return makeSendRequireError(new KVCommandResponse());
        }
        return task.thenApply(m -> (KVCommandResponse) m);
    }
    //endregion

    //region ====ReadIndex====
    public static CompletableFuture<KVGetResponse> execKVGetAsync(KVGetRequest req) {
        var task = makeTaskAndSendRequire(req);
        if (task == null) {
            return makeSendRequireError(new KVGetResponse());
        }
        return task.thenApply(m -> (KVGetResponse) m);
    }

    public static CompletableFuture<KVScanResponse> execKVScanAsync(KVScanRequest req) {
        var task = makeTaskAndSendRequire(req);
        if (task == null) {
            return makeSendRequireError(new KVScanResponse());
        }
        return task.thenApply(m -> (KVScanResponse) m);
    }
    //endregion
}
