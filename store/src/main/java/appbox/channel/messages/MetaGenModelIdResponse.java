package appbox.channel.messages;

import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.channel.MessageType;
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
    public void readFrom(BinDeserializer bs) {
        reqId     = bs.readInt();
        errorCode = bs.readInt();
        modelId   = bs.readInt();
    }
}
