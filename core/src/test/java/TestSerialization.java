import appbox.model.ApplicationModel;
import appbox.model.EntityModel;
import appbox.model.entity.DataFieldModel;
import appbox.model.entity.FieldWithOrder;
import appbox.model.entity.SysIndexModel;
import appbox.serialization.*;
import appbox.utils.IdUtil;
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
        long v      = 0x5AD1CCF440BA7000L;
        var  output = new BytesOutputStream(8);
        output.writeLong(v);

        var input = output.copyToInput();
        assertEquals(v, input.readLong());
    }

    @Test
    public void testSerialization() throws Exception {
        var output = new BytesOutputStream(1024);
        TestHelper.serializeTo(12345, output);

        var input = output.copyToInput();
        assertEquals(12345, TestHelper.deserializeFrom(input));
    }

    @Test
    public void testEntityModel() throws Exception {
        var model = TestHelper.makeEntityModel();

        //serialize
        var output1 = new BytesOutputStream(200);
        TestHelper.serializeTo(model, output1);
        //deserialize
        var input    = output1.copyToInput();
        var outModel = (EntityModel) TestHelper.deserializeFrom(input);
        //serialize again
        var output2 = new BytesOutputStream(200);
        TestHelper.serializeTo(outModel, output2);
        //assert
        assertEquals(output1.size(), output2.size());
        assertArrayEquals(output1.data, output2.data);
    }

}
