package appbox.exceptions;

public final class UnknownEntityMember extends RuntimeException {

    public UnknownEntityMember(Class<?> clazz, short memberId) {
        super(String.format("%s %d", clazz.getSimpleName(), memberId));
    }

}
