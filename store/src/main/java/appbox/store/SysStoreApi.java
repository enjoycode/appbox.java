package appbox.store;

import appbox.channel.IMessage;
import appbox.logging.Log;
import appbox.model.ApplicationModel;
import appbox.channel.IMessageChannel;
import appbox.channel.messages.*;
import appbox.runtime.RuntimeContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统存储的Api，通过消息通道与主进程的存储引擎交互
 * 注意: 所有响应不抛出异常，调用者根据响应的ErrorCode作相应的处理
 */
public final class SysStoreApi {

    /** 等待存储响应的请求 */
    public static final class PendingItem<T extends StoreResponse> {
        public CompletableFuture<T> future;
        public T                    response;

        public void completeAsync(Exception ex) {
            if (ex == null)
                future.completeAsync(() -> response);
            else
                CompletableFuture.runAsync(() -> future.completeExceptionally(ex));
        }
    }

    private static       IMessageChannel                                                  _channel;
    private static final ConcurrentHashMap<Integer, PendingItem<? extends StoreResponse>> _pendings
            = new ConcurrentHashMap<>(100);

    private SysStoreApi() {}

    public static void init(IMessageChannel channel) {
        _channel = channel;
    }

    /** 仅供收到系统存储响应时调用 */
    public static PendingItem<? extends StoreResponse> getPendingItem(int reqId) {
        return _pendings.remove(reqId);
    }

    /**
     * 根据请求消息新建异步任务加入挂起的字典表，并且序列化发送请求消息
     * @return 如果序列化或发送失败返回已失败的Future
     */
    private static <T extends StoreResponse> CompletableFuture<T> makeTaskAndSendRequest(IMessage request, T response) {
        var msgId   = _channel.newMessageId();
        var pending = new PendingItem<T>();
        pending.future   = new CompletableFuture<T>();
        pending.response = response;

        //注意处理当前会话信息，发送前获取，收到回复后恢复
        var session = RuntimeContext.current().currentSession();
        //加入等待列表 TODO:检测已存在，另考虑将会话信息一并放入字典表，在onResponse恢复会话
        _pendings.put(msgId, pending);
        try {
            _channel.sendMessage(msgId, request); //TODO:超时处理
        } catch (Exception e) {
            _pendings.remove(msgId);
            pending.future.completeExceptionally(e);
            Log.warn("发送请求消息[" + request.getClass().getName() + "]错误: " + e.getMessage());
            return pending.future;
        }

        //从Loop线程回到默认线程池继续执行
        return pending.future.thenApply(m -> {
            //Log.debug("收到存储响应: " + m.reqId + " at:" + Thread.currentThread().getName());
            //先恢复会话信息
            RuntimeContext.current().setCurrentSession(session);
            //再返回响应消息
            return m;
        });
    }

    //region ====Transaction====
    public static CompletableFuture<KVBeginTxnResponse> beginTxnAsync(/*TODO:isoLevel*/) {
        return makeTaskAndSendRequest(new KVBeginTxnRequire(), new KVBeginTxnResponse());
    }

    public static CompletableFuture<KVCommandResponse> commitTxnAsync(KVTxnId txnId) {
        var req = new KVEndTxnRequire();
        req.txnId.copyFrom(txnId);
        req.action = 0;
        return makeTaskAndSendRequest(req, new KVCommandResponse());
    }

    public static CompletableFuture<KVCommandResponse> rollbackTxnAsync(KVTxnId txnId) {
        var req = new KVEndTxnRequire();
        req.txnId.copyFrom(txnId);
        req.action = 1;
        return makeTaskAndSendRequest(req, new KVCommandResponse());
    }
    //endregion

    //region ====Meta====
    protected static CompletableFuture<MetaNewAppResponse> metaNewAppAsync(ApplicationModel app) {
        return makeTaskAndSendRequest(new MetaNewAppRequire(app), new MetaNewAppResponse());
    }

    protected static CompletableFuture<KVCommandResponse> metaNewBlobAsync(String storeName) {
        return makeTaskAndSendRequest(new MetaNewBlobRequest(storeName), new KVCommandResponse());
    }

    protected static CompletableFuture<MetaGenModelIdResponse> metaGenModelIdAsync(int appId, boolean devLayer) {
        var req = new MetaGenModelIdRequire(appId, devLayer);
        return makeTaskAndSendRequest(req, new MetaGenModelIdResponse());
    }

    protected static CompletableFuture<MetaGenPartitionResponse> metaGenPartitionAsync(PartitionInfo info, KVTxnId txnId) {
        var req = new MetaGenPartitionRequest(info, txnId);
        return makeTaskAndSendRequest(req, new MetaGenPartitionResponse());
    }
    //endregion

    //region ====KVCommands====
    public static CompletableFuture<KVCommandResponse> execKVInsertAsync(KVInsertRequire cmd) {
        return makeTaskAndSendRequest(cmd, new KVCommandResponse());
    }

    public static CompletableFuture<KVCommandResponse> execKVUpdateAsync(KVUpdateRequest cmd) {
        return makeTaskAndSendRequest(cmd, new KVCommandResponse());
    }

    public static CompletableFuture<KVCommandResponse> execKVDeleteAsync(KVDeleteRequest cmd) {
        return makeTaskAndSendRequest(cmd, new KVCommandResponse());
    }

    public static CompletableFuture<KVCommandResponse> execKVAddRefAsync(KVAddRefRequest cmd) {
        return makeTaskAndSendRequest(cmd, new KVCommandResponse());
    }
    //endregion

    //region ====ReadIndex====
    public static <T extends KVGetResponse> CompletableFuture<T> execKVGetAsync(KVGetRequest req, T res) {
        return makeTaskAndSendRequest(req, res);
    }

    public static <T extends KVScanResponse> CompletableFuture<T> execKVScanAsync(KVScanRequest req, T res) {
        return makeTaskAndSendRequest(req, res);
    }
    //endregion
}
