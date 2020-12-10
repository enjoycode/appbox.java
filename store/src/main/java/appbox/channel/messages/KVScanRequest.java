package appbox.channel.messages;

import appbox.channel.IMessage;
import appbox.channel.MessageType;
import appbox.serialization.IInputStream;

public abstract class KVScanRequest implements IMessage {

    @Override
    public byte MessageType() {
        return MessageType.KVScanRequest;
    }

    @Override
    public void readFrom(IInputStream bs) {
        throw new UnsupportedOperationException();
    }

}


