import appbox.store.utils.ModelCodeUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class TestModelCodeUtil {

    @Test
    public void testEncodeAndDecodeServiceCode() throws IOException {
        var src         = "Hello Future! 你好未来!Hello Future! 你好未来!";
        var encodedData = ModelCodeUtil.encodeServiceCode(src, true);

        Boolean isDeclare   = false;
        var     serviceCode = ModelCodeUtil.decodeServiceCode(encodedData);
        assertEquals(src, serviceCode.sourceCode);
        assertEquals(true, serviceCode.isDeclare);
    }

}
