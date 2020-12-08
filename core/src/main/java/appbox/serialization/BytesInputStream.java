package appbox.serialization;

public final class BytesInputStream implements IInputStream {
    public final byte[] data;
    private      int    pos;

    public BytesInputStream(int size) {
        data = new byte[size];
        pos  = 0;
    }

    public BytesInputStream(byte[] data) {
        this.data = data;
        pos       = 0;
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
        return data.length - pos;
    }

    @Override
    public byte readByte() {
        return data[pos++];
    }

    @Override
    public void read(byte[] dest, int offset, int count) {
        System.arraycopy(data, pos, dest, offset, count);
        pos += count;
    }
}