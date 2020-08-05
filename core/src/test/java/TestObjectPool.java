import appbox.core.cache.ObjectPool;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestObjectPool {

    class Person {
        String Name;
    }

    @Test
    void TestRentAndBack() {
        var pool = new ObjectPool<>(Person::new, 8);
        var p1 = pool.rent();
        p1.Name = "Rick";
        pool.back(p1);
        var p2 = pool.rent();

        assertSame(p1, p2);
    }

}
