package appbox.channel;

import com.sun.jna.Pointer;

/**
 * 与主进程通信的共享内存通道，每个实例包含两个单向消息队列
 */
public final class SharedMemoryChannel implements IMessageChannel, AutoCloseable {
    private final Pointer _sendQueue;
    private final Pointer _sendBufferPtr;
    private final Pointer _receiveQueue;
    private final Pointer _receiveBufferPtr;

    public SharedMemoryChannel(String name) {
        // 注意与主进程的名称相反
        _receiveQueue = NativeSmq.SMQ_Open(name + "-S");
        _sendQueue = NativeSmq.SMQ_Open(name + "-R");
        _receiveBufferPtr = NativeSmq.SMQ_GetBufferPtr(_receiveQueue);
        _sendBufferPtr = NativeSmq.SMQ_GetBufferPtr(_sendQueue);
    }

    @Override
    public void close() throws Exception {

    }

    /**
     * 开始在当前线程接收消息
     */
    public void startReceive() {
        int msgNo = 0;
        int resLen = 13;

        while (true) {
            var rnode = NativeSmq.SMQ_GetNodeForReading(_receiveQueue, -1);
            var rchunk = _receiveBufferPtr.share(NativeSmq.getNodeOffset(rnode));
            if (NativeSmq.getMsgType(rchunk) == -128) { //收到退出消息
                break;
            }
            var rdata = NativeSmq.getDataPtr(rchunk);
            var rshard = rdata.getShort(0); // Require Shard

            var wnode = NativeSmq.SMQ_GetNodeForWriting(_sendQueue, -1);
            var wchunk = _sendBufferPtr.share(NativeSmq.getNodeOffset(wnode));
            // 写消息头
            NativeSmq.setMsgFirst(wchunk, wchunk);
            NativeSmq.setMsgNext(wchunk, Pointer.NULL);
            NativeSmq.setMsgId(wchunk, msgNo++);
            NativeSmq.setMsgType(wchunk, (byte) 11); // InvokeResponse
            NativeSmq.setMsgFlag(wchunk, (byte) 12); // First | Last
            NativeSmq.setMsgDataLen(wchunk, (short) (4 + 2 + resLen));
            // 写消息体
            var wdata = NativeSmq.getDataPtr(wchunk);
            wdata.setInt(0, NativeSmq.getMsgId(rchunk));
            wdata.setShort(4, rshard);
            wdata.setMemory(6, resLen, (byte) 65); // 'A'

            NativeSmq.SMQ_ReturnNode(_receiveQueue, rnode);
            NativeSmq.SMQ_PostNode(_sendQueue, wnode);
        }
    }

}
