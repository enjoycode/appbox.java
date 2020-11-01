import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

public class TestFastJson {

    @Test
    public void testSerializeWriter() {
        var sr = new StringWriter();
        var s = new SerializeWriter(sr);
        s.writeFieldValue('{', "name", "rick");
        s.writeFieldValue(',', "age", 21);
        s.flush();
    }

}
