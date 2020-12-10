package appbox.runtime;

import appbox.cache.BytesSegment;
import appbox.serialization.IOutputStream;
import appbox.serialization.PayloadType;

public final class InvokeArgs {

    public static class Maker implements IOutputStream {

        private BytesSegment  _current;
        private int           _pos;

        Maker() {
            _current    = BytesSegment.rent();
        }

        public void add(int arg) {
            writeByte(PayloadType.Int32);
            writeInt(arg);
        }

        public InvokeArgs done() {
            return new InvokeArgs(_current.first());
        }

        //region ----IOutputStream----
        @Override
        public void writeByte(byte value) {
            if (_pos >= BytesSegment.FRAME_SIZE) {
                _current = BytesSegment.rent(_current);
                _pos = 0;
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
                    _current = BytesSegment.rent(_current);
                    _pos = 0;
                    write(src, offset + left, count - left);
                }
            } else {
                _current = BytesSegment.rent(_current);
                _pos = 0;
                write(src, offset, count);
            }
        }
        //endregion
    }

    private BytesSegment _current;

    private InvokeArgs(BytesSegment first) {
       _current = first;
    }

}
