import appbox.model.EntityModel;
import appbox.serialization.*;
import org.junit.jupiter.api.Test;
import testutils.TestHelper;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class TestSerialization {

    @Test
    public void testUtf8EncodeAndDecode() {
        var s = "中A";
        try {
            var bytes = s.getBytes(StandardCharsets.UTF_8);
            assertEquals(bytes.length, 4);
            var d = new String(bytes, 0, bytes.length, StandardCharsets.UTF_8);
            assertEquals(s, d);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testStream() {
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
    public void testLong() {
        long v      = 0x5AD1CCF440BA7000L;
        var  output = new BytesOutputStream(8);
        output.writeLong(v);

        var input = output.copyToInput();
        assertEquals(v, input.readLong());
    }

    @Test
    public void testSerialization() {
        var output = new BytesOutputStream(1024);
        output.serialize(12345);

        var input = output.copyToInput();
        assertEquals(12345, input.deserialize());
    }

    @Test
    public void testEntityModel() {
        var model = TestHelper.makeEntityModel();

        //serialize
        var output1 = new BytesOutputStream(200);
        output1.serialize(model);
        //deserialize
        var input    = output1.copyToInput();
        var outModel = (EntityModel) input.deserialize();
        //serialize again
        var output2 = new BytesOutputStream(200);
        output2.serialize(outModel);
        //assert
        assertEquals(output1.size(), output2.size());
        assertArrayEquals(output1.toByteArray(), output2.toByteArray());
    }

    @Test
    public void testStoreVarLen() {
        int len = 154;
        var out = new BytesOutputStream(8);
        out.writeStoreVarLen(len);

        var input   = out.copyToInput();
        int varSize = input.readStoreVarLen();
        assertEquals(len, varSize);
    }

}
