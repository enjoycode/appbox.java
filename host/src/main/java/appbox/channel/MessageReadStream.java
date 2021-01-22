package appbox.channel;

import appbox.cache.BytesSegment;
import appbox.cache.ObjectPool;
import appbox.data.Entity;
import appbox.runtime.InvokeArgs;
import appbox.serialization.IInputStream;
import com.sun.jna.Pointer;

import java.util.ArrayList;
import java.util.List;

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
        if (obj._deserialized != null)
            obj._deserialized.clear();
        pool.back(obj);
    }
    //endregion

    private Pointer      _curChunk;
    private Pointer      _dataPtr;
    private int          _dataLen;
    private int          _index;
    private List<Entity> _deserialized;

    protected void reset(Pointer first) {
        _curChunk = first;
        _dataPtr  = NativeSmq.getDataPtr(_curChunk);
        _dataLen  = NativeSmq.getMsgDataLen(_curChunk);
        _index    = 0;
    }

    /** 复制剩余部分(已读取消息头)作为Invoke参数 */
    protected InvokeArgs copyToArgs() {
        var segment = BytesSegment.rent();
        _dataPtr.read(_index, segment.buffer, 0, _dataLen - _index);
        segment.setDataSize(_dataLen - _index);

        var temp = NativeSmq.getMsgNext(_curChunk);
        while (temp != Pointer.NULL) {
            segment = BytesSegment.rent(segment);
            var dataPtr = NativeSmq.getDataPtr(temp);
            var dataLen = NativeSmq.getMsgDataLen(temp);
            dataPtr.read(0, segment.buffer, 0, dataLen);
            segment.setDataSize(dataLen);

            temp = NativeSmq.getMsgNext(temp);
        }
        return new InvokeArgs(segment.first());
    }

    /**
     * 当前块的剩余字节数
     */
    private int left() {
        return _dataLen - _index;
    }

    private void moveToNext() {
        var next = NativeSmq.getMsgNext(_curChunk);
        if (next == Pointer.NULL) {
            throw new RuntimeException("Has no data to read.");
        }

        reset(next);
    }

    @Override
    public boolean hasRemaining() {
        return left() > 0 || NativeSmq.getMsgNext(_curChunk) != Pointer.NULL;
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
    public byte readByte() {
        if (left() <= 0) {
            moveToNext();
        }
        return _dataPtr.getByte(_index++);
    }

    @Override
    public void readBytes(byte[] dest, int offset, int count) {
        var left = left();
        if (left > 0) {
            if (left >= count) {
                _dataPtr.read(_index, dest, offset, count);
                _index += count;
            } else {
                _dataPtr.read(_index, dest, offset, left);
                moveToNext();
                readBytes(dest, offset + left, count - left);
            }
        } else {
            moveToNext();
            readBytes(dest, offset, count);
        }
    }

    @Override
    public short readShort() {
        if (left() >= 2) {
            var res = _dataPtr.getShort(_index);
            _index += 2;
            return res;
        }
        return IInputStream.super.readShort();
    }

    @Override
    public void addToDeserialized(Entity obj) {
        if (_deserialized == null)
            _deserialized = new ArrayList<>();
        _deserialized.add(obj);
    }

    @Override
    public Entity getDeserialized(int index) {
        return _deserialized.get(index);
    }

}
