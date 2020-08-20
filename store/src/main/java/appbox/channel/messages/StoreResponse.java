package appbox.channel.messages;

import appbox.channel.IMessage;

public abstract class StoreResponse implements IMessage {
    public int reqId;
    public int errorCode;
}
