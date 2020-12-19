package appbox.runtime;

import appbox.cache.BytesSegment;
import appbox.data.Entity;
import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;
import appbox.serialization.PayloadType;

import java.util.function.Supplier;

public final class InvokeArgs implements IInputStream {

    //region ====Maker====
    public static class Maker implements IOutputStream {

        private BytesSegment _current;
        private int          _pos;

        private Maker() {
            _current = BytesSegment.rent();
        }

        public InvokeArgs done() {
            _current.setDataSize(_pos);
            return new InvokeArgs(_current.first());
        }

        public Maker add(int arg) {
            serialize(arg);
            return this;
        }

        public Maker add(String arg) {
            serialize(arg);
            return this;
        }

        //region ----IOutputStream----
        private void createSegment() {
            _current.setDataSize(_pos);
            _current = BytesSegment.rent(_current);
            _pos     = 0;
        }

        @Override
        public void writeByte(byte value) {
            if (_pos >= BytesSegment.FRAME_SIZE) {
                createSegment();
            }
            _current.buffer[_pos++] = value;
        }

        @Override
        public void write(byte[] src, int offset, int count) {
            var left = BytesSegment.FRAME_SIZE - _pos;
            if (left > 0) {
                if (left >= count) {
                    System.arraycopy(src, offset, _current.buffer, _pos, count);
                    _pos += count;
                } else {
                    System.arraycopy(src, offset, _current.buffer, _pos, left);
                    createSegment();
                    write(src, offset + left, count - left);
                }
            } else {
                createSegment();
                write(src, offset, count);
            }
        }
        //endregion
    }

    public static Maker make() {
        return new Maker();
    }
    //endregion

    private BytesSegment _current;
    private int          _pos;

    public InvokeArgs(BytesSegment first) {
        if (first == null || first.first() != first)
            throw new IllegalArgumentException("segment is null or not is first");
        _current = first;
    }

    public void free() {
        BytesSegment.backAll(_current);
    }

    //region ====GetXXX Methods====
    public boolean getBool() {
        var payloadType = readByte();
        if (payloadType == PayloadType.BooleanTrue) {
            return true;
        } else if (payloadType == PayloadType.BooleanFalse) {
            return false;
        }
        throw new RuntimeException("PayloadType Error");
    }

    public int getInt() {
        var payloadType = readByte();
        if (payloadType == PayloadType.Int32) {
            return readInt();
        }
        throw new RuntimeException("PayloadType Error");
    }

    public long getLong() {
        var payloadType = readByte();
        if (payloadType == PayloadType.Int32) {
            return readInt();
        } else if (payloadType == PayloadType.Int64) {
            return readLong();
        }
        throw new RuntimeException("PayloadType Error");
    }

    public String getString() {
        var payloadType = readByte();
        if (payloadType == PayloadType.String) {
            return readString();
        }
        throw new RuntimeException("PayloadType Error");
    }

    public <E extends Entity> E getEntity(Supplier<E> creator) {
        var payloadType = readByte();
        if (payloadType == PayloadType.Null)
            return null;
        if (payloadType == PayloadType.Entity) {
            var obj = creator.get();
            obj.readFrom(this);
            return obj;
        }
        throw new RuntimeException("PayloadType Error");
    }
    //endregion

    //region ====IInputStream====
    private void moveToNext() {
        var next = _current.next();
        if (next == null) {
            throw new RuntimeException("Has no data to read.");
        }

        _current = next;
        _pos     = 0;
    }

    @Override
    public byte readByte() {
        if (_pos >= _current.getDataSize()) {
            moveToNext();
        }
        return _current.buffer[_pos++];
    }

    @Override
    public void read(byte[] dest, int offset, int count) {
        var left = _current.getDataSize() - _pos;
        if (left > 0) {
            if (left >= count) {
                System.arraycopy(_current.buffer, _pos, dest, offset, count);
                _pos += count;
            } else {
                System.arraycopy(_current.buffer, _pos, dest, offset, left);
                moveToNext();
                read(dest, offset + left, count - left);
            }
        } else {
            moveToNext();
            read(dest, offset, count);
        }
    }

    @Override
    public int remaining() {
        var remaining = _current.getDataSize() - _pos;
        var temp      = _current.next();
        while (temp != null) {
            remaining += temp.getDataSize();
            temp = temp.next();
        }
        return remaining;
    }
    //endregion

}
