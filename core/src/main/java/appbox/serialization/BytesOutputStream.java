package appbox.serialization;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 仅用于测试
 */
public final class BytesOutputStream implements IOutputStream {
    public final byte[] data;
    private      int    index;

    public BytesOutputStream(int size) {
        data  = new byte[size];
        index = 0;
    }

    public int size() {
        return index;
    }

    public BytesInputStream copyToInput() {
        var input = new BytesInputStream(index);
        System.arraycopy(data, 0, input.data, 0, index);
        return input;
    }

    public void saveToFile(int offset, String file) throws IOException {
        var os = new FileOutputStream(file);
        try {
            os.write(data, offset, index);
            os.flush();
        } finally {
            os.close();
        }
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
