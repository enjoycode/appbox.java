package appbox.core.serialization;

/**
 * 仅用于测试
 */
public final class BytesInputStream implements IInputStream {
    public final byte[] data;
    private       int    index;

    public BytesInputStream(int size) {
        data  = new byte[size];
        index = 0;
    }

    @Override
    public byte readByte() throws Exception {
        return data[index++];
    }

    @Override
    public void read(byte[] dest, int offset, int count) throws Exception {
        System.arraycopy(data, index, dest, offset, count);
        index += count;
    }
}