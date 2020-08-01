package appbox.channel;

import com.sun.jna.Pointer;

/**
 * 消息读取流，用于从消息链中读取完整消息
 */
public final class MessageReadStream {
    private Pointer _curChunk;
    private Pointer _dataPtr;
    private int     _index;

    public void reset(Pointer first) {
        _curChunk = first;
        _dataPtr  = NativeSmq.getDataPtr(_curChunk);
        _index    = 0;
    }

    private int left() {
        return NativeSmq.CHUNK_DATA_SIZE - _index;
    }

    private void moveToNext() throws Exception {
        var next = NativeSmq.getMsgNext(_curChunk);
        if (next == Pointer.NULL) {
            throw new Exception("Has no data to read.");
        }

        reset(next);
    }

    public byte readByte() throws Exception {
        if (left() <= 0) {
            moveToNext();
        }
        return _dataPtr.getByte(_index++);
    }

    public short readShort() throws Exception {
        if (left() >= 2) {
            var res = _dataPtr.getShort(_index);
            _index += 2;
            return res;
        }
        return (short) (readByte() << 8 | readByte());
    }
}
