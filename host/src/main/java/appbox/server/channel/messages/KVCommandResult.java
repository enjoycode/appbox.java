package appbox.server.channel.messages;

import appbox.core.serialization.BinDeserializer;
import appbox.core.serialization.BinSerializer;
import appbox.server.channel.MessageType;

public final class KVCommandResult implements IMessage {
    @Override
    public byte MessageType() {
        return MessageType.KVCommandResult;
    }

    @Override
    public void writeTo(BinSerializer bs) throws Exception {

    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {

    }
}
