package appbox.serialization;

import java.io.ByteArrayInputStream;

public final class BytesInputStream extends ByteArrayInputStream implements IInputStream {

    public BytesInputStream(byte[] data) {
        super(data);
    }

    public int getPosition() { return pos; }

    @Override
    public void skip(int size) {
        if (size < 0)
            throw new IllegalArgumentException();
        pos += size;
    }

    @Override
    public int remaining() {
        return buf.length - pos;
    }

    @Override
    public byte readByte() {
        return buf[pos++];
    }

    @Override
    public void readBytes(byte[] dest, int offset, int count) {
        System.arraycopy(buf, pos, dest, offset, count);
        pos += count;
    }

}
