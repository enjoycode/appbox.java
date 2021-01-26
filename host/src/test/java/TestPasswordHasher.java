import appbox.server.security.PasswordHasher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestPasswordHasher {

    @Test
    public void hashAndVerify() {
        var haser = new PasswordHasher();

        var pwd    = "123456";
        var hashed = haser.hashPassword(pwd);
        assertTrue(haser.verifyHashedPassword(hashed, pwd));
        assertFalse(haser.verifyHashedPassword(hashed, "123457"));
    }

}
