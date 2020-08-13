package appbox.server.channel.messages;

import appbox.core.serialization.BinDeserializer;
import appbox.core.serialization.BinSerializer;
import appbox.server.channel.MessageType;

public final class NewAppResponse implements IMessage {
    @Override
    public byte MessageType() {
        return MessageType.NewAppResponse;
    }

    public int  reqId;
    public byte appId;

    //region ====Serialization====
    @Override
    public void writeTo(BinSerializer bs) throws Exception {

    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {

    }
    //endregion
}
