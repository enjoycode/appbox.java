package appbox.channel.messages;

import appbox.channel.IMessage;
import appbox.channel.MessageType;

public abstract class KVScanRequest implements IMessage {

    @Override
    public byte MessageType() {
        return MessageType.KVScanRequest;
    }

}


