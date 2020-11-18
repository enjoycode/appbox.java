package appbox.channel.messages;

import appbox.channel.IMessage;
import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.channel.MessageType;

public final class MetaGenModelIdRequire implements IMessage {
    public int     appId;
    public boolean devLayer;

    @Override
    public byte MessageType() {
        return MessageType.MetaGenModelIdRequire;
    }

    @Override
    public void writeTo(BinSerializer bs) {
        bs.writeInt(appId);
        bs.writeBool(devLayer);
    }

    @Override
    public void readFrom(BinDeserializer bs) {

    }
}
