package appbox.serialization;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public final class BytesOutputStream extends ByteArrayOutputStream implements IOutputStream {

    private SerializeContext _ctx;

    public BytesOutputStream(int size) {
        super(size);
    }

    public byte[] getBuffer() {return buf;}

    public BytesInputStream copyToInput() { //TODO:move to input
        var dest = new byte[count];
        System.arraycopy(buf, 0, dest, 0, count);
        return new BytesInputStream(dest);
    }

    public void saveToFile(int offset, String file) throws IOException {
        try (var os = new FileOutputStream(file)) {
            os.write(buf, offset, count);
            os.flush();
        }
    }

    @Override
    public SerializeContext getContext() {
        if (_ctx == null)
            _ctx = new SerializeContext();
        return _ctx;
    }

    @Override
    public void writeByte(byte value) {
        super.write(value);
    }

}
