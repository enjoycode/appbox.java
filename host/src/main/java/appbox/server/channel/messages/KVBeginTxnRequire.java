package appbox.server.channel.messages;

import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.server.channel.MessageType;

public final class KVBeginTxnRequire implements IMessage {
    //TODO:ObjectPool

    public byte isoLevel;

    @Override
    public byte MessageType() {
        return MessageType.KVBeginTxnRequire;
    }

    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        bs.writeByte(isoLevel);
    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {

    }
}
