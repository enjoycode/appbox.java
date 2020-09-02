package appbox.channel;

import appbox.runtime.ISessionInfo;
import appbox.runtime.RuntimeContext;
import appbox.channel.messages.*;
import appbox.logging.Log;
import appbox.server.runtime.HostRuntimeContext;
import appbox.store.SysStoreApi;
import com.sun.jna.Pointer;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * 消息分发处理器，处理来自主进程的消息
 */
public final class MessageDispatcher {

    /**
     * 处理通道接收的消息，注意：当前线程为接收Loop内，由实现者决定在哪个线程执行具体的操作
     *
     * @param channel 接收消息的通道
     * @param first   完整消息的第一包
     */
    public static void processMessage(IHostMessageChannel channel, Pointer first) {
        switch (NativeSmq.getMsgType(first)) {
            case MessageType.InvokeRequire:
                processInvokeRequire(channel, first);
                break;
            case MessageType.MetaNewAppResponse:
                processStoreResponse(channel, first, new MetaNewAppResponse());
                break;
            case MessageType.KVBeginTxnResponse:
                processStoreResponse(channel, first, new KVBeginTxnResponse());
                break;
            case MessageType.KVCommandResponse:
                processStoreResponse(channel, first, new KVCommandResponse());
                break;
            case MessageType.KVGetResponse:
                processStoreResponse(channel, first, new KVGetResponse());
                break;
            case MessageType.KVScanResponse:
                processStoreResponse(channel, first, new KVScanResponse());
                break;
            default:
                channel.returnAllChunks(first);
                Log.warn("Receive unknown type message.");
                break;
        }
    }

    private static void processInvokeRequire(IHostMessageChannel channel, Pointer first) {
        //根据协议类型反序列化消息
        var req = InvokeRequire.rentFromPool();
        req.reqId = NativeSmq.getMsgId(first);
        Exception deserializeError = null;
        try {
            IHostMessageChannel.deserialize(req, first);
        } catch (Exception e) {
            deserializeError = e;
        } finally {
            channel.returnAllChunks(first);
        }

        if (deserializeError == null) {
            //异步交给运行时服务容器处理
            CompletableFuture.supplyAsync(() -> {
                //先设置当前会话信息
                ISessionInfo sessionInfo = null;
                if (req.sessionId != 0) {
                    sessionInfo = SessionManager.tryGet(req.sessionId);
                }
                ((HostRuntimeContext) RuntimeContext.current()).setCurrentSession(sessionInfo);
                //再调用服务
                return RuntimeContext.invokeAsync(req.service, req.args);
            }).thenCompose(r -> r).handle((r, ex) -> {
                //发送请求响应
                var res = InvokeResponse.rentFromPool();
                res.reqId  = req.reqId;
                res.shard  = req.shard;
                res.error  = ex == null ? InvokeResponse.ErrorCode.None : InvokeResponse.ErrorCode.ServiceInnerError;
                res.result = ex == null ? r : ex.getMessage();
                try {
                    channel.sendMessage(channel.newMessageId(), res);
                    if (ex != null) {
                        Log.error("Invoke Service[" + req.service
                                + "] Error:" + ex.getMessage() + "\n" + Arrays.toString(ex.getStackTrace()));
                    }
                } catch (Exception e) {
                    Log.warn("发送响应消息失败");
                } finally {
                    InvokeRequire.backToPool(req);
                    InvokeResponse.backToPool(res);
                }

                return null;
            });
        } else { //反序列化错误直接发送响应
            Exception finalDeserializeError = deserializeError;
            CompletableFuture.runAsync(() -> {
                var res = InvokeResponse.rentFromPool();
                res.reqId  = req.reqId;
                res.shard  = req.shard;
                res.error  = InvokeResponse.ErrorCode.DeserializeRequestFail;
                res.result = finalDeserializeError.getMessage();

                try {
                    channel.sendMessage(channel.newMessageId(), res);
                } catch (Exception e) {
                    Log.warn("发送响应消息失败");
                } finally {
                    InvokeRequire.backToPool(req);
                    InvokeResponse.backToPool(res);
                }

                Log.warn("反序列化InvokeRequire错误: " + finalDeserializeError.getMessage());
            });
        }
    }

    private static <T extends StoreResponse> void processStoreResponse(IHostMessageChannel channel, Pointer first, T res) {
        //Log.debug(NativeSmq.getDebugInfo(first, true));

        boolean isDeserializeError = false;
        try {
            IHostMessageChannel.deserialize(res, first);
        } catch (Exception e) {
            isDeserializeError = true;
        } finally {
            channel.returnAllChunks(first);
        }

        if (!isDeserializeError) {
            CompletableFuture.runAsync(() -> {
                SysStoreApi.onResponse(res.reqId, res);
            });
        } else {
            CompletableFuture.runAsync(() -> {
                SysStoreApi.onResponseDeserializeError(res.reqId);
                //TODO:res back to pool, if it is pooled.
            });
            Log.warn("反序列化StoreResponse错误");
        }
    }
}
