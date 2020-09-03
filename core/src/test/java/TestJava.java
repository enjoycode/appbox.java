import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestJava {

    @Test
    public void testReflection() {
        Object v1   = 1;
        var    type = v1.getClass();
        assertNotNull(type);

        String a = "Hello";
        String b = a;
        b += " Future";
        assertEquals("Hello", a);
    }

    @Test
    public void testCharSequence() {
        //var s0 = "Hello";
        //String.class.getConstructor()
        var s1  = "中Hello World";
        var s2  = "中Hello Future";
        var sq1 = s1.subSequence(0, 5);
        var sq2 = s2.subSequence(0, 5);
        assertEquals(sq1, sq2);
        assertEquals(sq1.hashCode(), sq2.hashCode());

        char[] arr = {'r', 'u', 'n', 'o', 'o', 'b'};
        var    s3  = String.valueOf(arr);
    }

    @Test
    void testUnsigned() {
        int  a  = -12345;
        long b  = -12345678L;
        var  a1 = Integer.toUnsignedString(a);
        var  b1 = Long.toUnsignedString(b);
        assertEquals(a, Integer.parseUnsignedInt(a1));
        assertEquals(b, Long.parseUnsignedLong(b1));
    }

}
