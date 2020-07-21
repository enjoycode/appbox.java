import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestTestLibrary {

    @Test
    void testLibraryMethod() {
        TestLibrary classUnderTest = new TestLibrary();
        assertTrue(classUnderTest.sayHello(), "should return true");
    }
}
