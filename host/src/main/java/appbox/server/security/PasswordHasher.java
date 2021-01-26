package appbox.server.security;

import appbox.runtime.IPasswordHasher;

import java.util.Arrays;
import java.util.Random;

public final class PasswordHasher implements IPasswordHasher {

    @Override
    public byte[] hashPassword(String password) {
        if (password == null || password.isEmpty())
            throw new IllegalArgumentException();

        var salt   = new byte[16];
        var random = new Random();
        random.nextBytes(salt);

        try {
            var rfc = new Rfc2898DeriveBytes(password, salt, 1000);
            var res = new byte[49];
            res[0] = 0; //保留1字节类型
            System.arraycopy(salt, 0, res, 1, 16);
            var hash = rfc.getBytes(32);
            System.arraycopy(hash, 0, res, 17, 32);
            return res;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean verifyHashedPassword(byte[] hashedPassword, String password) {
        if (password == null || password.isEmpty())
            throw new IllegalArgumentException();

        if (hashedPassword == null || hashedPassword.length != 49 || hashedPassword[0] != 0)
            return false;

        var salt = new byte[16];
        System.arraycopy(hashedPassword, 1, salt, 0, 16);
        try {
            var rfc  = new Rfc2898DeriveBytes(password, salt, 1000);
            var hash = rfc.getBytes(32);
            return Arrays.equals(hash, 0, 32, hashedPassword, 17, 49);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
