package appbox.server.channel.messages;

import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.server.channel.MessageType;

public final class MetaGenModelIdResponse extends StoreResponse {
    public int errorCode;
    public int modelId;

    @Override
    public byte MessageType() {
        return MessageType.MetaGenModelIdResponse;
    }

    @Override
    public void writeTo(BinSerializer bs) throws Exception {

    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {
        reqId     = bs.readInt();
        errorCode = bs.readInt();
        modelId   = bs.readInt();
    }
}
