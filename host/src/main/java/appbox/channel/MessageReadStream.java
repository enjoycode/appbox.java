package appbox.channel;

import appbox.cache.ObjectPool;
import appbox.serialization.IInputStream;
import com.sun.jna.Pointer;

/**
 * 消息读取流，用于从消息链中读取完整消息
 */
public final class MessageReadStream implements IInputStream {
    //region ====ObjectPool====
    private static final ObjectPool<MessageReadStream> pool = new ObjectPool<>(MessageReadStream::new, 32);

    public static MessageReadStream rentFromPool(Pointer first) {
        var obj = pool.rent();
        obj.reset(first);
        return obj;
    }

    public static void backToPool(MessageReadStream obj) {
        pool.back(obj);
    }
    //endregion

    private Pointer _curChunk;
    private Pointer _dataPtr;
    private int     _dataLen;
    private int     _index;

    private void reset(Pointer first) {
        _curChunk = first;
        _dataPtr  = NativeSmq.getDataPtr(_curChunk);
        _dataLen  = NativeSmq.getMsgDataLen(_curChunk);
        _index    = 0;
    }

    /**
     * 当前块的剩余字节数
     */
    private int left() {
        return _dataLen - _index;
    }

    private void moveToNext() throws Exception {
        var next = NativeSmq.getMsgNext(_curChunk);
        if (next == Pointer.NULL) {
            throw new Exception("Has no data to read.");
        }

        reset(next);
    }

    @Override
    public int remaining() {
        var remaining = left();
        var temp      = NativeSmq.getMsgNext(_curChunk);
        while (temp != Pointer.NULL) {
            remaining += NativeSmq.getMsgDataLen(temp);
            temp = NativeSmq.getMsgNext(temp);
        }
        return remaining;
    }

    @Override
    public byte readByte() throws Exception {
        if (left() <= 0) {
            moveToNext();
        }
        return _dataPtr.getByte(_index++);
    }

    @Override
    public void read(byte[] dest, int offset, int count) throws Exception {
        var left = left();
        if (left > 0) {
            if (left >= count) {
                _dataPtr.read(_index, dest, offset, count);
                _index += count;
            } else {
                _dataPtr.read(_index, dest, offset, left);
                moveToNext();
                read(dest, offset + left, count - left);
            }
        } else {
            moveToNext();
            read(dest, offset, count);
        }
    }

    @Override
    public short readShort() throws Exception {
        if (left() >= 2) {
            var res = _dataPtr.getShort(_index);
            _index += 2;
            return res;
        }
        return IInputStream.super.readShort();
    }
}
