import appbox.serialization.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestSerialization {

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
        output.writeInt(0x0A0B0C0D);
        output.writeVariant(-1);
        output.writeVariant(123);
        output.writeVariant(Integer.MAX_VALUE);
        output.writeVariant(Integer.MIN_VALUE);
        output.writeString("中A");

        var input = output.copyToInput();
        assertEquals(0x0A0B0C0D, input.readInt());
        assertEquals(-1, input.readVariant());
        assertEquals(123, input.readVariant());
        assertEquals(Integer.MAX_VALUE, input.readVariant());
        assertEquals(Integer.MIN_VALUE, input.readVariant());
        assertEquals("中A", input.readString());
    }

    @Test
    public void testLong() throws Exception {
        long v = 0x5AD1CCF440BA7000L;
        var output = new BytesOutputStream(8);
        output.writeLong(v);

        var input = output.copyToInput();
        assertEquals(v, input.readLong());
    }

    @Test
    public void testSerialization() throws Exception {
        var output = new BytesOutputStream(8192);
        var os     = BinSerializer.rentFromPool(output);
        os.serialize(12345);
        BinSerializer.backToPool(os);

        var input = output.copyToInput();
        var is    = BinDeserializer.rentFromPool(input);
        assertEquals(12345, is.deserialize());
        BinDeserializer.backToPool(is);
    }

}
