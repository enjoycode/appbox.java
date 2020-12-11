package appbox.channel;

import appbox.cache.ObjectPool;
import appbox.serialization.IOutputStream;
import com.sun.jna.Pointer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 消息发送流, 注意目前实现边写边发
 */
public final class MessageWriteStream extends OutputStream implements IOutputStream /*暂继承OutputStream方便写Json*/ {

    //region ====ObjectPool====
    private static final ObjectPool<MessageWriteStream> pool = new ObjectPool<>(MessageWriteStream::new, 32);

    public static MessageWriteStream rentFromPool(byte msgType, int msgId, long sourceId, byte msgFlag,
                                                  Supplier<Pointer> maker, Consumer<Pointer> sender) {
        var obj = pool.rent();
        obj._curChunk = null; //必须设置，否则缓存重用有问题
        obj._msgType  = msgType;
        obj._msgId    = msgId;
        obj._sourceId = sourceId;
        obj._msgFlag  = msgFlag;

        obj._maker  = maker;
        obj._sender = sender;

        obj.createChunk();
        return obj;
    }

    public static void backToPool(MessageWriteStream obj) {
        pool.back(obj);
    }
    //endregion

    private Pointer _curChunk;
    private Pointer _dataPtr;
    private int     _index;

    private byte _msgType;
    private int  _msgId;
    private byte _msgFlag;
    private long _sourceId;

    private Supplier<Pointer> _maker;
    private Consumer<Pointer> _sender;

    private void createChunk() {
        var preChunk = _curChunk;
        _curChunk = _maker.get();
        //初始化包的消息头
        NativeSmq.setMsgType(_curChunk, _msgType);
        NativeSmq.setMsgId(_curChunk, _msgId);
        NativeSmq.setMsgFlag(_curChunk, _msgFlag);
        NativeSmq.setMsgSource(_curChunk, _sourceId);
        NativeSmq.setMsgDataLen(_curChunk, (short) NativeSmq.CHUNK_DATA_SIZE);
        //设置消息链表
        NativeSmq.setMsgNext(_curChunk, Pointer.NULL);
        if (preChunk == Pointer.NULL) {
            NativeSmq.setMsgFirst(_curChunk, _curChunk);
            NativeSmq.setMsgFlag(_curChunk, (byte) (_msgFlag | MessageFlag.FirstChunk));
        } else {
            NativeSmq.setMsgNext(preChunk, _curChunk);
            NativeSmq.setMsgFirst(_curChunk, NativeSmq.getMsgFirst(preChunk));
        }
        //设置数据指针
        _dataPtr = NativeSmq.getDataPtr(_curChunk);
        _index   = 0;
        //直接发送前一包
        if (_sender != null && preChunk != Pointer.NULL) {
            _sender.accept(preChunk);
        }
    }

    public void finish() {
        //设置当前消息包的长度
        NativeSmq.setMsgDataLen(_curChunk, (short) _index);
        //将当前消息包标为完整消息结束
        NativeSmq.setMsgFlag(_curChunk, (byte) (NativeSmq.getMsgFlag(_curChunk) | MessageFlag.LastChunk));
        //直接发送最后一包
        if (_sender != null) {
            _sender.accept(_curChunk);
            _curChunk = Pointer.NULL;
        }
    }

    /**
     * 用于发生错误时将当前消息转换为取消状态，并发送给接收端，由接收端丢弃其他包
     */
    protected void flushWhenCancelled() {
        NativeSmq.setMsgDataLen(_curChunk, (short) 0);
        NativeSmq.setMsgFlag(_curChunk, (byte) (NativeSmq.getMsgFlag(_curChunk)
                | MessageFlag.LastChunk | MessageFlag.SerializeError));
        if (_sender != null)
            _sender.accept(_curChunk);
    }

    @Override
    public void write(int value) throws IOException {
        writeByte((byte) value);
    }

    @Override
    public void writeByte(byte value) {
        if (_index >= NativeSmq.CHUNK_DATA_SIZE) {
            createChunk();
        }
        _dataPtr.setByte(_index++, value);
    }

    @Override
    public void write(byte[] src, int offset, int count) {
        var left = NativeSmq.CHUNK_DATA_SIZE - _index;
        if (left > 0) {
            if (left >= count) {
                _dataPtr.write(_index, src, offset, count);
                _index += count;
            } else {
                _dataPtr.write(_index, src, offset, left);
                createChunk();
                write(src, offset + left, count - left);
            }
        } else {
            createChunk();
            write(src, offset, count);
        }
    }
}
