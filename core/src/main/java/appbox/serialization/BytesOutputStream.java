package appbox.serialization;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public final class BytesOutputStream extends ByteArrayOutputStream implements IOutputStream {

    public BytesOutputStream(int size) {
        super(size);
    }

    public BytesInputStream copyToInput() {
        var input = new BytesInputStream(super.count);
        System.arraycopy(super.buf, 0, input.data, 0, super.count);
        return input;
    }

    public void saveToFile(int offset, String file) throws IOException {
        var os = new FileOutputStream(file);
        try {
            os.write(super.buf, offset, super.count);
            os.flush();
        } finally {
            os.close();
        }
    }

    @Override
    public void writeByte(byte value) {
        super.write(value);
    }

}
