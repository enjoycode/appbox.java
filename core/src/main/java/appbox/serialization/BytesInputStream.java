package appbox.serialization;

/**
 * 仅用于测试
 */
public final class BytesInputStream implements IInputStream {
    public final byte[] data;
    private      int    pos;

    public BytesInputStream(int size) {
        data = new byte[size];
        pos  = 0;
    }

    @Override
    public int remaining() {
        return data.length - pos;
    }

    @Override
    public byte readByte() throws Exception {
        return data[pos++];
    }

    @Override
    public void read(byte[] dest, int offset, int count) throws Exception {
        System.arraycopy(data, pos, dest, offset, count);
        pos += count;
    }
}