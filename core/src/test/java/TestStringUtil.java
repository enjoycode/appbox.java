import appbox.core.utils.StringUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestStringUtil {
    @Test
    public void testGetStringHashCode() {
        var s = "Hello中";
        int hs1 = StringUtil.getHashCode(s);
        int hs2 = s.hashCode();
        assertNotEquals(hs1, hs2);
    }
}
