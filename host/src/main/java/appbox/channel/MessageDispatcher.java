package appbox.channel;

import appbox.runtime.IUserSession;
import appbox.runtime.InvokeArgs;
import appbox.runtime.RuntimeContext;
import appbox.channel.messages.*;
import appbox.logging.Log;
import appbox.store.SysStoreApi;
import com.sun.jna.Pointer;

import java.util.concurrent.CompletableFuture;

/** 消息分发处理器，处理来自主进程的消息 */
public final class MessageDispatcher {

    /** 专用于Loop线程内解析消息,重复使用 */
    private static final MessageReadStream msgReaderOnLoop = new MessageReadStream();

    /**
     * 处理通道接收的消息，注意：当前线程为接收Loop线程内，由实现者决定在哪个线程执行具体的操作
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
            case MessageType.MetaGenModelIdResponse:
            case MessageType.MetaGenPartitionResponse:
            case MessageType.KVBeginTxnResponse:
            case MessageType.CommonResponse:
            case MessageType.KVGetResponse:
            case MessageType.KVScanResponse:
                processStoreResponse(channel, first);
                break;
            case MessageType.StartDebuggerResponse:
                processStartDebugResponse(channel, first);
                break;
            case MessageType.StopDebuggerRequest:
                processStopDebugRequest(channel, first);
                break;
            default:
                channel.returnAllChunks(first);
                Log.warn("Receive unknown type message: " + msgType);
                break;
        }
    }

    private static void processInvokeRequire(IHostMessageChannel channel, Pointer first) {
        //读取请求消息头
        final int reqId = NativeSmq.getMsgId(first);
        msgReaderOnLoop.reset(first);
        final short shard = msgReaderOnLoop.readShort();
        try {
            final long       sessionId = msgReaderOnLoop.readLong();
            final String     service   = msgReaderOnLoop.readString();
            final InvokeArgs args      = msgReaderOnLoop.hasRemaining() ? msgReaderOnLoop.copyToArgs() : null;

            //异步交给运行时服务容器处理
            CompletableFuture.runAsync(() -> {
                //先设置当前会话信息
                final IUserSession sessionInfo = sessionId == 0 ? null : SessionManager.tryGet(sessionId);
                RuntimeContext.current().setCurrentSession(sessionInfo);
                //再调用服务,注意必须捕获处理异常(如NoClassDefFoundError)
                try {
                    RuntimeContext.invokeAsync(service, args).handle((r, ex) -> {
                        var error  = ex == null ? InvokeResponse.ErrorCode.None : InvokeResponse.ErrorCode.ServiceInnerError;
                        var result = ex == null ? r : ex.getMessage();
                        //发送请求响应
                        sendInvokeResponse(channel, shard, reqId, error, result);

                        if (ex != null) {
                            Log.error(String.format("Invoke service[%s] error: %s", service, ex));
                            ex.printStackTrace(); //TODO: to log
                        }
                        //注意返回InvokeArgs所租用的BytesSegment
                        if (args != null)
                            args.free();
                        return null;
                    });
                } catch (Throwable ex) {
                    CompletableFuture.runAsync(() -> {
                        sendInvokeResponse(channel, shard, reqId,
                                InvokeResponse.ErrorCode.ServiceInnerError, ex.getMessage());
                        Log.error(String.format("Invoke service[%s] error: %s", service, ex));
                    });
                }
            });
        } catch (Exception e) {
            //反序列化错误直接发送响应并返回
            CompletableFuture.runAsync(() -> {
                sendInvokeResponse(channel, shard, reqId,
                        InvokeResponse.ErrorCode.DeserializeRequestFail, e.getMessage());
                Log.warn("Deserialize InvokeRequest error: " + e.getMessage());
            });
        } finally {
            channel.returnAllChunks(first);
        }
    }

    private static void sendInvokeResponse(IHostMessageChannel channel, short shard, int reqId
            , byte errorCode, Object result) {
        var res = InvokeResponse.rentFromPool();
        res.reqId  = reqId;
        res.shard  = shard;
        res.error  = errorCode;
        res.result = result;

        try {
            channel.sendMessage(channel.newMessageId(), res);
        } catch (Exception e) {
            Log.warn("Send InvokeResponse error: " + e.getMessage());
        } finally {
            InvokeResponse.backToPool(res);
        }
    }

    private static void processStoreResponse(IHostMessageChannel channel, Pointer first) {
        var reqId = NativeSmq.getDataPtr(first).getInt(0); ;
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
            pendingItem.completeAsync(ex);
            Log.warn("反序列化StoreResponse错误: ");
            ex.printStackTrace();
            return;
        } finally {
            channel.returnAllChunks(first);
        }

        //正常响应，注意必须异步，还在Loop线程内
        pendingItem.completeAsync(null);
    }

    private static void processStartDebugResponse(IHostMessageChannel channel, Pointer first) {
        //反序列化响应
        var response = new StartDebugResponse();
        try {
            IHostMessageChannel.deserialize(response, first);
        } catch (Exception ex) {
            response.errorCode = 2;
            Log.warn("Deserialize [StartDebugResponse] error: " + ex);
        } finally {
            channel.returnAllChunks(first);
        }

        CompletableFuture.runAsync(() -> DebugSessionManager.onStartResponse(response));
    }

    private static void processStopDebugRequest(IHostMessageChannel channel, Pointer first) {
        var request = new StopDebugRequest();
        try {
            IHostMessageChannel.deserialize(request, first);
        } catch (Exception ex) {
            Log.warn("Deserialize [StopDebugRequest] error: " + ex);
        } finally {
            channel.returnAllChunks(first);
        }

        CompletableFuture.runAsync(() -> DebugSessionManager.onStopRequest(request.sessionId));
    }

}
