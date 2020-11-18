package appbox.channel;

import appbox.logging.Log;
import appbox.serialization.BinSerializer;
import com.sun.jna.Pointer;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 与主进程通信的共享内存通道，每个实例包含两个单向消息队列
 */
public final class SharedMemoryChannel implements IHostMessageChannel, AutoCloseable {
    private final Pointer                   _sendQueue;    //发送队列
    private final Pointer                   _receiveQueue; //接收队列
    private final HashMap<Integer, Pointer> _pendings;     //挂起的不完整消息
    private final AtomicInteger             _msgNo;        //发送消息流水号

    public SharedMemoryChannel(String name) {
        // 注意与主进程的名称相反
        _receiveQueue = NativeSmq.SMQ_Open(name + "-S");
        _sendQueue    = NativeSmq.SMQ_Open(name + "-R");
        _pendings     = new HashMap<>();
        _msgNo        = new AtomicInteger();
    }

    @Override
    public void returnAllChunks(Pointer first) {
        NativeSmq.SMQ_ReturnAllChunks(_receiveQueue, first);
    }

    @Override
    public void close() {

    }

    /**
     * 开始在当前线程接收消息
     */
    public void startReceive() {
        //int msgNo  = 0;
        //int resLen = 13;

        while (true) {
            var rchunk = NativeSmq.SMQ_GetChunkForReading(_receiveQueue, -1);
            if (NativeSmq.getMsgType(rchunk) == MessageType.ExitReadLoop) { //收到退出消息
                break;
            }

            onMessageChunk(rchunk);

            //====以下回传测试====
            //var rid    = NativeSmq.getMsgId(rchunk);
            //var rdata  = NativeSmq.getDataPtr(rchunk);
            //var rshard = rdata.getShort(0); // Require Shard
            //NativeSmq.SMQ_ReturnChunk(_receiveQueue, rchunk);
            //
            //var wid = msgNo++;
            //
            //// Debug模式: 同步9万/秒，异步7.2万/秒; Release模式: 同步14.3万/秒，异步11.9万/秒
            //CompletableFuture.runAsync(() -> {
            //    var wchunk = NativeSmq.SMQ_GetChunkForWriting(_sendQueue, -1);
            //    // 写消息头
            //    NativeSmq.setMsgFirst(wchunk, wchunk);
            //    NativeSmq.setMsgNext(wchunk, Pointer.NULL);
            //    NativeSmq.setMsgId(wchunk, wid);
            //    NativeSmq.setMsgType(wchunk, (byte) 11); // InvokeResponse
            //    NativeSmq.setMsgFlag(wchunk, (byte) 12); // First | Last
            //    NativeSmq.setMsgDataLen(wchunk, (short) (4 + 2 + resLen));
            //    // 写消息体
            //    var wdata = NativeSmq.getDataPtr(wchunk);
            //    wdata.setInt(0, rid); // 原请求标识
            //    wdata.setShort(4, rshard); // 原请求Shard
            //    wdata.setMemory(6, resLen, (byte) 65); // 'A'
            //
            //    NativeSmq.SMQ_PostChunk(_sendQueue, wchunk);
            //});
        }
    }

    /**
     * 收到消息开始组合为完整的消息
     */
    private void onMessageChunk(Pointer chunk) { //TODO:特殊类型消息(KVScanResponse)流式处理
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

    @Override
    public int newMessageId() {
        return _msgNo.incrementAndGet();
    }

    /**
     * 序列化并发送消息，如果序列化异常标记消息为错误状态仍旧发送,接收端根据消息类型是请求还是响应作不同处理
     */
    @Override
    public <T extends IMessage> void sendMessage(int id, T msg) {
        byte flag     = MessageFlag.None;
        long sourceId = 0; //TODO:fix

        var mws = MessageWriteStream.rentFromPool(msg.MessageType(),
                id, sourceId, flag,
                () -> NativeSmq.SMQ_GetChunkForWriting(_sendQueue, -1),
                (s) -> NativeSmq.SMQ_PostChunk(_sendQueue, s));
        var bs = BinSerializer.rentFromPool(mws);
        try {
            msg.writeTo(bs);
            mws.flush(); //必须
        } catch (Exception e) {
            //发生异常，则通知接收端取消挂起的消息
            mws.flushWhenCancelled();
            //记录日志并重新抛出异常
            Log.warn(e.toString());
            throw e;
        } finally {
            BinSerializer.backToPool(bs);
            MessageWriteStream.backToPool(mws);
        }
    }

}
