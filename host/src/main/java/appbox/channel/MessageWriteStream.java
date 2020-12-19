package appbox.channel;

import appbox.cache.ObjectPool;
import appbox.logging.Log;
import appbox.serialization.IOutputStream;
import com.sun.jna.Pointer;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 消息发送流, 注意目前实现边写边发
 */
final class MessageWriteStream extends OutputStream implements IOutputStream /*暂继承OutputStream方便写Json*/ {

    //region ====ObjectPool====
    private static final ObjectPool<MessageWriteStream> pool = new ObjectPool<>(MessageWriteStream::new, 32);

    public static MessageWriteStream rentFromPool(byte msgType, int msgId, long sourceId, byte msgFlag,
                                                  Pointer sendQueue) {
        var obj = pool.rent();
        obj.initChunk(msgType, msgId, sourceId, msgFlag);
        obj._sender = sendQueue;
        return obj;
    }

    public static void backToPool(MessageWriteStream obj) {
        obj._sender = Pointer.NULL;
        obj._first  = Pointer.NULL;
        pool.back(obj);
    }
    //endregion


    private final byte[]  _buf = new byte[NativeSmq.CHUNK_SIZE];
    private       int     _index;
    private       Pointer _sender;
    private       Pointer _first;

    private void initChunk(byte msgType, int msgId, long sourceId, byte msgFlag) {
        _index = NativeSmq.CHUNK_HEAD_SIZE;

        //初始化包的消息头
        _buf[0] = msgType;
        _buf[1] = msgFlag;

        _buf[2] = (byte) (NativeSmq.CHUNK_DATA_SIZE & 0xFF);
        _buf[3] = (byte) ((NativeSmq.CHUNK_DATA_SIZE >> 8) & 0xFF);

        _buf[4] = (byte) (msgId & 0xFF);
        _buf[5] = (byte) ((msgId >> 8) & 0xFF);
        _buf[6] = (byte) ((msgId >> 16) & 0xFF);
        _buf[7] = (byte) ((msgId >> 24) & 0xFF);

        _buf[8]  = (byte) (sourceId & 0xFF);
        _buf[9]  = (byte) ((sourceId >> 8) & 0xFF);
        _buf[10] = (byte) ((sourceId >> 16) & 0xFF);
        _buf[11] = (byte) ((sourceId >> 24) & 0xFF);
        _buf[12] = (byte) ((sourceId >> 32) & 0xFF);
        _buf[13] = (byte) ((sourceId >> 40) & 0xFF);
        _buf[14] = (byte) ((sourceId >> 48) & 0xFF);
        _buf[15] = (byte) ((sourceId >> 56) & 0xFF);
    }

    private void sendChunk(boolean isLast, boolean isCancelled) {
        var chunk = NativeSmq.SMQ_GetChunkForWriting(_sender, -1);
        //复制数据给MessageChunk
        if (isCancelled)
            chunk.write(0, _buf, 0, NativeSmq.CHUNK_HEAD_SIZE);
        else
            chunk.write(0, _buf, 0, _index);

        byte flag = _buf[1];
        if (_first == Pointer.NULL) {
            _first = chunk;
            flag |= MessageFlag.FirstChunk;
            if (isLast) {
                flag |= MessageFlag.LastChunk;
                NativeSmq.setMsgDataLen(chunk, (short) (_index - NativeSmq.CHUNK_HEAD_SIZE));
            }
            NativeSmq.setMsgFlag(chunk, flag);
        } else if (isLast) {
            flag |= MessageFlag.LastChunk;
            if (isCancelled) {
                flag |= MessageFlag.SerializeError;
                NativeSmq.setMsgDataLen(chunk, (short) 0);
            } else {
                NativeSmq.setMsgDataLen(chunk, (short) (_index - NativeSmq.CHUNK_HEAD_SIZE));
            }
            NativeSmq.setMsgFlag(chunk, flag);
        }
        //直接发送,不用设置消息链表,接收端会处理
        //Log.debug(NativeSmq.getDebugInfo(chunk, false));
        NativeSmq.SMQ_PostChunk(_sender, chunk);

        _index = NativeSmq.CHUNK_HEAD_SIZE;
    }

    /**
     * 完成消息写入，发送最后一包
     * @param isCancelled 用于发生错误时将当前消息转换为取消状态，并发送给接收端，由接收端丢弃其他包
     */
    public void finish(boolean isCancelled) {
        sendChunk(true, isCancelled);
    }

    @Override
    public void write(int value) throws IOException {
        writeByte((byte) value);
    }

    @Override
    public void writeByte(byte value) {
        if (_index >= NativeSmq.CHUNK_SIZE) {
            sendChunk(false, false);
        }
        _buf[_index++] = value;
    }

    @Override
    public void write(byte[] src, int offset, int count) {
        var left = NativeSmq.CHUNK_SIZE - _index;
        if (left > 0) {
            if (left >= count) {
                System.arraycopy(src, offset, _buf, _index, count);
                _index += count;
            } else {
                System.arraycopy(src, offset, _buf, _index, left);
                _index += left;
                sendChunk(false, false);
                write(src, offset + left, count - left);
            }
        } else {
            sendChunk(false, false);
            write(src, offset, count);
        }
    }
}
