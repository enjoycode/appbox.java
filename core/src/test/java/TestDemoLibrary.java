import appbox.core.DemoLibrary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestDemoLibrary {

    @Test
    void testLibraryMethod() {
        var classUnderTest = new DemoLibrary();
        assertTrue(classUnderTest.sayHello(), "should return true");
    }
}
