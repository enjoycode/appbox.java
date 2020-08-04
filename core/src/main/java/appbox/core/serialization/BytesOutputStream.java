package appbox.core.serialization;

/**
 * 仅用于测试
 */
public final class BytesOutputStream implements IOutputStream {
    public final byte[] data;
    private       int    index;

    public BytesOutputStream(int size) {
        data  = new byte[size];
        index = 0;
    }

    public BytesInputStream copyTo() {
        var input = new BytesInputStream(index);
        System.arraycopy(data, 0, input.data, 0, index);
        return input;
    }

    @Override
    public void writeByte(byte value) {
        data[index++] = value;
    }

    @Override
    public void write(byte[] src, int offset, int count) {
        System.arraycopy(src, offset, data, index, count);
        index += count;
    }
}
