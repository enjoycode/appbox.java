import appbox.store.KeyUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestKeyUtil {

    @Test
    public void testEncodeTableId() {
        byte appId = 0x01;
        int tableId = 0x00020304;
        int encoded = KeyUtil.encodeTableId(appId, tableId);
        assertEquals(0x04030201, encoded);
    }

}
