package appbox.server.channel.messages;

import appbox.core.serialization.BinDeserializer;
import appbox.core.serialization.BinSerializer;
import appbox.server.channel.MessageType;

public final class MetaNewAppResponse extends StoreResponse {
    public int  errorCode;
    public byte appId;

    @Override
    public byte MessageType() {
        return MessageType.MetaNewAppResponse;
    }

    //region ====Serialization====
    @Override
    public void writeTo(BinSerializer bs) throws Exception {

    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {
        reqId     = bs.readInt();
        errorCode = bs.readInt();
        appId     = bs.readByte();
    }
    //endregion
}
