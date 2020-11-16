package appbox.channel.messages;

import appbox.channel.IMessage;
import appbox.store.SysStoreException;

/** 系统存储的响应基类 */
public abstract class StoreResponse implements IMessage {
    public int reqId;
    public int errorCode;

    public final void checkStoreError() throws SysStoreException {
        if (errorCode != 0) {
            throw new SysStoreException(errorCode);
        }
    }

}
