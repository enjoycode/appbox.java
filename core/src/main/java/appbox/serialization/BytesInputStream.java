package appbox.serialization;

import appbox.data.Entity;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public final class BytesInputStream extends ByteArrayInputStream implements IInputStream {

    private List<Entity> _deserialized;

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

    @Override
    public void addToDeserialized(Entity obj) {
        if (_deserialized == null)
            _deserialized = new ArrayList<>();
        _deserialized.add(obj);
    }

    @Override
    public Entity getDeserialized(int index) {
        return _deserialized.get(index);
    }
}
