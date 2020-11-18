package appbox.channel.messages;

import appbox.channel.IMessage;
import appbox.channel.MessageType;
import appbox.serialization.BinDeserializer;

public abstract class KVScanRequest implements IMessage {

    @Override
    public byte MessageType() {
        return MessageType.KVScanRequest;
    }

    @Override
    public void readFrom(BinDeserializer bs) {
        throw new UnsupportedOperationException();
    }

}


