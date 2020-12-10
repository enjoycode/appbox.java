package appbox.channel.messages;

import appbox.channel.IMessage;
import appbox.channel.MessageType;
import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;

public final class MetaGenModelIdRequire implements IMessage {
    public int     appId;
    public boolean devLayer;

    @Override
    public byte MessageType() {
        return MessageType.MetaGenModelIdRequire;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeInt(appId);
        bs.writeBool(devLayer);
    }

    @Override
    public void readFrom(IInputStream bs) {

    }
}
