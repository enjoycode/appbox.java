package appbox.channel.messages;

import appbox.channel.IMessage;

/** 系统存储的响应基类 */
public abstract class StoreResponse implements IMessage {
    public int reqId;
    public int errorCode;
}
