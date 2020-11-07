package appbox.channel;

import appbox.runtime.ISessionInfo;
import appbox.runtime.RuntimeContext;
import appbox.channel.messages.*;
import appbox.logging.Log;
import appbox.store.SysStoreApi;
import com.sun.jna.Pointer;

import java.util.concurrent.CompletableFuture;

/**
 * 消息分发处理器，处理来自主进程的消息
 */
public final class MessageDispatcher {

    /**
     * 处理通道接收的消息，注意：当前线程为接收Loop内，由实现者决定在哪个线程执行具体的操作
     * @param channel 接收消息的通道
     * @param first   完整消息的第一包
     */
    public static void processMessage(IHostMessageChannel channel, Pointer first) {
        var msgType = NativeSmq.getMsgType(first);
        switch (msgType) {
            case MessageType.InvokeRequire:
                processInvokeRequire(channel, first);
                break;
            case MessageType.MetaNewAppResponse:
            case MessageType.MetaGenPartitionResponse:
            case MessageType.KVBeginTxnResponse:
            case MessageType.KVCommandResponse:
            case MessageType.KVGetResponse:
            case MessageType.KVScanResponse:
                processStoreResponse(channel, first);
                break;
            default:
                channel.returnAllChunks(first);
                Log.warn("Receive unknown type message: " + msgType);
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
                RuntimeContext.current().setCurrentSession(sessionInfo);
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
                        Log.error(String.format("Invoke Service[%s] Error:%s", req.service, ex.getMessage()));
                        ex.getCause().printStackTrace(); //TODO: to log
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

    private static void processStoreResponse(IHostMessageChannel channel, Pointer first) {
        var reqId = NativeSmq.getDataPtr(first).getInt(0);;
        var pendingItem = SysStoreApi.getPendingItem(reqId);
        if (pendingItem == null) { //可能已超时
            channel.returnAllChunks(first);
            Log.warn("收到存储引擎响应时找不到相应的请求:" + reqId);
            return;
        }

        //反序列化响应
        try {
            IHostMessageChannel.deserialize(pendingItem.response, first);
        } catch (Exception ex) {
            CompletableFuture.runAsync(() -> pendingItem.future.completeExceptionally(ex)); //同样必须异步
            Log.warn("反序列化StoreResponse错误: ");
            ex.printStackTrace();
            return;
        } finally {
            channel.returnAllChunks(first);
        }

        //正常响应，注意必须异步，还在Loop线程内
        pendingItem.completeAsync();
    }

}
