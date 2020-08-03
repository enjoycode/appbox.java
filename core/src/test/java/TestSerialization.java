import appbox.core.serialization.IInputStream;
import appbox.core.serialization.IOutputStream;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Array;

import static org.junit.jupiter.api.Assertions.*;

public class TestSerialization {

    class BytesOutputStream implements IOutputStream {
        private final byte[] data;
        private int index;

        BytesOutputStream(int size) {
            data = new byte[size];
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
        public void write(byte[] value) {
            System.arraycopy(value, 0, data, index, value.length);
            index += value.length;
        }
    }

    class BytesInputStream implements IInputStream {
        private final byte[] data;
        private int index;

        BytesInputStream(int size) {
            data = new byte[size];
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

    @Test
    public void testUtf8EncodeAndDecode() {
        var s = "中A";
        try {
            var bytes = s.getBytes("UTF-8");
            assertEquals(bytes.length, 4);
            var d = new String(bytes, 0, bytes.length, "UTF-8");
            assertEquals(s, d);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testStream() throws Exception {
        var output = new BytesOutputStream(8192);
        output.writeVariant(-1);
        output.writeString("中A");

        var input = output.copyTo();
        assertEquals( -1, input.readVariant());
        assertEquals("中A", input.readString());
    }
}
