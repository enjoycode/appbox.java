import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestTestLibrary {

    @Test
    void testLibraryMethod() {
        var classUnderTest = new appbox.core.TestLibrary();
        assertTrue(classUnderTest.sayHello(), "should return true");
    }
}
