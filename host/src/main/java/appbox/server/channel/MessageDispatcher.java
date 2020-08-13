package appbox.server.channel;

import appbox.core.runtime.RuntimeContext;
import appbox.server.channel.messages.InvokeRequire;
import appbox.server.channel.messages.InvokeResponse;
import appbox.core.logging.Log;
import appbox.server.channel.messages.NewAppResponse;
import appbox.store.SysStoreApi;
import com.sun.jna.Pointer;

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
    public static void processMessage(IMessageChannel channel, Pointer first) {
        switch (NativeSmq.getMsgType(first)) {
            case MessageType.InvokeRequire:
                processInvokeRequire(channel, first);
                break;
            case MessageType.NewAppResponse:
                processNewAppResponse(channel, first);
                break;
            default:
                channel.returnAllChunks(first);
                Log.warn("Receive unknown type message.");
                break;
        }
    }

    private static void processInvokeRequire(IMessageChannel channel, Pointer first) {
        //根据协议类型反序列化消息
        var req = InvokeRequire.rentFromPool();
        req.reqId = NativeSmq.getMsgId(first);
        boolean isDeserializeError = false;
        try {
            IMessageChannel.deserialize(req, first);
        } catch (Exception e) {
            InvokeRequire.backToPool(req); //失败归还
            isDeserializeError = true;
        } finally {
            channel.returnAllChunks(first);
        }

        if (!isDeserializeError) {
            //异步交给运行时服务容器处理
            CompletableFuture.runAsync(() -> {
                RuntimeContext.invokeAsync(req.service, req.args).thenAccept(r -> {
                    InvokeRequire.backToPool(req);

                    //发送请求响应
                    var res = InvokeResponse.rentFromPool();
                    res.reqId  = req.reqId;
                    res.shard  = req.shard;
                    res.error  = InvokeResponse.ErrorCode.None;
                    res.result = r;
                    try {
                        channel.sendMessage(channel.newMessageId(), res);
                    } catch (Exception e) {
                        Log.warn("发送响应消息失败");
                    } finally {
                        InvokeResponse.backToPool(res);
                    }
                }); //TODO:异常处理释放相应的缓存
            });
        } else {
            //TODO: 发送反序列化失败错误给调用者
        }
    }

    private static void processNewAppResponse(IMessageChannel channel, Pointer first) {
        var     res                = new NewAppResponse();
        boolean isDeserializeError = false;
        try {
            IMessageChannel.deserialize(res, first);
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
            //TODO:
        }
    }
}
