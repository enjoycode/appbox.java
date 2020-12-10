package appbox.channel.messages;

import appbox.channel.MessageType;
import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;

public final class MetaGenModelIdResponse extends StoreResponse {
    public int modelId;

    @Override
    public byte MessageType() {
        return MessageType.MetaGenModelIdResponse;
    }

    @Override
    public void writeTo(IOutputStream bs) {

    }

    @Override
    public void readFrom(IInputStream bs) {
        reqId     = bs.readInt();
        errorCode = bs.readInt();
        modelId   = bs.readInt();
    }
}
