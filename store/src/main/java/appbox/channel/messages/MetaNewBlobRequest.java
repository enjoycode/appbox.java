package appbox.channel.messages;

import appbox.channel.IMessage;
import appbox.channel.MessageType;
import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;

public final class MetaNewBlobRequest implements IMessage {
    private final String _storeName;

    public MetaNewBlobRequest(String storeName) {
        _storeName = storeName;
    }

    @Override
    public byte MessageType() { return MessageType.MetaNewBlobRequest;}

    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeUtf8(_storeName);
    }

    @Override
    public void readFrom(IInputStream bs) {
        throw new RuntimeException("Not supported");
    }
}
