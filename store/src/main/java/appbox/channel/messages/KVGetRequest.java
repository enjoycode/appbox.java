package appbox.channel.messages;

import appbox.channel.IMessage;
import appbox.channel.MessageType;

public abstract class KVGetRequest implements IMessage {

    @Override
    public byte MessageType() {
        return MessageType.KVGetRequest;
    }

}
