package appbox.channel.messages;

import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.channel.MessageType;

public final class MetaNewAppResponse extends StoreResponse {
    public byte appId;

    @Override
    public byte MessageType() {
        return MessageType.MetaNewAppResponse;
    }

    //region ====Serialization====
    @Override
    public void writeTo(BinSerializer bs) {

    }

    @Override
    public void readFrom(BinDeserializer bs) {
        reqId     = bs.readInt();
        errorCode = bs.readInt();
        appId     = bs.readByte();
    }
    //endregion
}
