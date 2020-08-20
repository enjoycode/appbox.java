package appbox.store;

/**
 * 系统存储的异常
 */
public final class SysStoreException extends RuntimeException {

    public final int errorCode;

    public SysStoreException(int errorCode) {
        this.errorCode = errorCode;
    }

}
