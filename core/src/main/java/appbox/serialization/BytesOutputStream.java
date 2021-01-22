package appbox.serialization;

import appbox.data.Entity;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class BytesOutputStream extends ByteArrayOutputStream implements IOutputStream {

    private List<Entity> _serializedList;

    public BytesOutputStream(int size) {
        super(size);
    }

    public byte[] getBuffer() { return buf; }

    public BytesInputStream copyToInput() { //TODO:move to input
        var dest = new byte[count];
        System.arraycopy(buf, 0, dest, 0, count);
        return new BytesInputStream(dest);
    }

    public void saveToFile(int offset, String file) throws IOException {
        var os = new FileOutputStream(file);
        try {
            os.write(buf, offset, count);
            os.flush();
        } finally {
            os.close();
        }
    }

    @Override
    public void writeByte(byte value) {
        super.write(value);
    }

    @Override
    public int getSerializedIndex(Entity obj) {
        if (_serializedList == null || _serializedList.size() == 0)
            return -1;
        for (int i = _serializedList.size() - 1; i >= 0; i--) {
            if (_serializedList.get(i) == obj)
                return i;
        }
        return -1;
    }

    @Override
    public void addToSerialized(Entity obj) {
        if (_serializedList == null)
            _serializedList = new ArrayList<>();
        _serializedList.add(obj);
    }

}
