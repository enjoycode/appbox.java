package appbox.channel.messages;

import appbox.channel.IMessage;
import appbox.channel.MessageType;
import appbox.serialization.IInputStream;

public abstract class KVGetRequest implements IMessage {

    @Override
    public byte MessageType() {
        return MessageType.KVGetRequest;
    }

    @Override
    public void readFrom(IInputStream bs) {
        throw new UnsupportedOperationException();
    }

}
