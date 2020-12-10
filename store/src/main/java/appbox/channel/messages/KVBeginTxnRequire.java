package appbox.channel.messages;

import appbox.channel.IMessage;
import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.channel.MessageType;
import appbox.serialization.IOutputStream;

public final class KVBeginTxnRequire implements IMessage {
    //TODO:ObjectPool

    public byte isoLevel;

    @Override
    public byte MessageType() {
        return MessageType.KVBeginTxnRequire;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeByte(isoLevel);
    }

    @Override
    public void readFrom(BinDeserializer bs) {
        throw new UnsupportedOperationException();
    }
}
