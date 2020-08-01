package appbox.channel;

import appbox.core.logging.Log;
import com.sun.jna.Pointer;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

/**
 * 与主进程通信的共享内存通道，每个实例包含两个单向消息队列
 */
public final class SharedMemoryChannel implements IMessageChannel, AutoCloseable {
    private final Pointer                   _sendQueue;    //发送队列
    private final Pointer                   _receiveQueue; //接收队列
    private final HashMap<Integer, Pointer> _pendings;     //挂起的不完整消息

    public SharedMemoryChannel(String name) {
        // 注意与主进程的名称相反
        _receiveQueue = NativeSmq.SMQ_Open(name + "-S");
        _sendQueue    = NativeSmq.SMQ_Open(name + "-R");
        _pendings     = new HashMap<>();
    }

    @Override
    public void returnAllChunks(Pointer first) {
        NativeSmq.SMQ_ReturnAllChunks(_receiveQueue, first);
    }

    @Override
    public void close() throws Exception {

    }

    /**
     * 开始在当前线程接收消息
     */
    public void startReceive() {
        int msgNo  = 0;
        int resLen = 13;

        while (true) {
            var rchunk = NativeSmq.SMQ_GetChunkForReading(_receiveQueue, -1);
            if (NativeSmq.getMsgType(rchunk) == MessageType.ExitReadLoop) { //收到退出消息
                break;
            }
            var rid    = NativeSmq.getMsgId(rchunk);
            var rdata  = NativeSmq.getDataPtr(rchunk);
            var rshard = rdata.getShort(0); // Require Shard
            NativeSmq.SMQ_ReturnChunk(_receiveQueue, rchunk);

            var wid = msgNo++;

            // Debug模式: 同步9万/秒，异步7.2万/秒; Release模式: 同步14.3万/秒，异步11.9万/秒
            CompletableFuture.runAsync(() -> {
                var wchunk = NativeSmq.SMQ_GetChunkForWriting(_sendQueue, -1);
                // 写消息头
                NativeSmq.setMsgFirst(wchunk, wchunk);
                NativeSmq.setMsgNext(wchunk, Pointer.NULL);
                NativeSmq.setMsgId(wchunk, wid);
                NativeSmq.setMsgType(wchunk, (byte) 11); // InvokeResponse
                NativeSmq.setMsgFlag(wchunk, (byte) 12); // First | Last
                NativeSmq.setMsgDataLen(wchunk, (short) (4 + 2 + resLen));
                // 写消息体
                var wdata = NativeSmq.getDataPtr(wchunk);
                wdata.setInt(0, rid); // 原请求标识
                wdata.setShort(4, rshard); // 原请求Shard
                wdata.setMemory(6, resLen, (byte) 65); // 'A'

                NativeSmq.SMQ_PostChunk(_sendQueue, wchunk);
            });

        }
    }

    /**
     * 收到消息开始组合为完整的消息
     */
    private void onMessageChunk(Pointer chunk) {
        var msgId   = NativeSmq.getMsgId(chunk);
        var msgFlag = NativeSmq.getMsgFlag(chunk);

        var isFirst = (msgFlag & MessageFlag.FirstChunk) == MessageFlag.FirstChunk;
        var isLast  = (msgFlag & MessageFlag.LastChunk) == MessageFlag.LastChunk;

        NativeSmq.setMsgNext(chunk, Pointer.NULL); //下一包始终置空
        if (isFirst) {
            NativeSmq.setMsgFirst(chunk, chunk); //首包设为自己
            if (isLast) { //单包直接处理
                processMessage(chunk);
            } else {
                //TODO:判断已存在取消掉
                _pendings.put(msgId, chunk);
            }
        } else {
            var preChunk = _pendings.get(msgId);
            if (preChunk == null) { //直接丢弃该包消息
                NativeSmq.SMQ_ReturnChunk(_receiveQueue, chunk);
                Log.warn("Receive message without first.");
            } else {
                var first = NativeSmq.getMsgFirst(preChunk);
                NativeSmq.setMsgFirst(chunk, first);
                NativeSmq.setMsgNext(preChunk, chunk);
                if (isLast) { //收到完整消息交给消息处理器
                    _pendings.remove(msgId);
                    processMessage(first);
                } else {
                    _pendings.replace(msgId, chunk); //重置消息Id的尾包
                }
            }
        }
    }

    private void processMessage(Pointer first) {
        //注意：除特殊消息(eg: CancelMessage)外交给MessageDispatcher处理
        MessageDispatcher.processMessage(this, first);
    }

}
