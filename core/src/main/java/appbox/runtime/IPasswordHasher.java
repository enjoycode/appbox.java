package appbox.runtime;

public interface IPasswordHasher {

    byte[] hashPassword(String password);

    boolean verifyHashedPassword(byte[] hashedPassword, String password);

}
