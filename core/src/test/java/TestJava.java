import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestJava {

    @Test
    public void testReflection() {
        Object v1 = 1;
        var type = v1.getClass();
        assertNotNull(type);

        String a = "Hello";
        String b = a;
        b += " Future";
        assertEquals("Hello", a);
    }

}
