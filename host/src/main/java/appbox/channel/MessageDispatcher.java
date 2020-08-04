package appbox.channel;

import appbox.channel.messages.InvokeRequire;
import appbox.core.logging.Log;
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
            MessageSerializer.deserialize(req, first);
        } catch (Exception e) {
            //TODO: 发送反序列化失败错误给调用者
            InvokeRequire.backToPool(req); //失败归还
            isDeserializeError = true;
        } finally {
            channel.returnAllChunks(first);
        }

        if (!isDeserializeError) {
            //异步交给运行时服务容器处理
            CompletableFuture.runAsync(() -> {
                Log.info(req.service); //TODO:别忘了归还InvokeRequire
            });
        }
    }

}
