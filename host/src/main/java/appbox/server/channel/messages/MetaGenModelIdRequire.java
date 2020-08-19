package appbox.server.channel.messages;

import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.server.channel.MessageType;

public final class MetaGenModelIdRequire implements IMessage {
    public int     appId;
    public boolean devLayer;

    @Override
    public byte MessageType() {
        return MessageType.MetaGenModelIdRequire;
    }

    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        bs.writeInt(appId);
        bs.writeBool(devLayer);
    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {

    }
}
