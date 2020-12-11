package appbox.channel.messages;

import appbox.channel.IMessage;
import appbox.channel.MessageType;
import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;

public final class MetaGenModelIdRequire implements IMessage {
    private final int     appId;
    private final boolean devLayer;

    public MetaGenModelIdRequire(int appId, boolean devLayer) {
        this.appId    = appId;
        this.devLayer = devLayer;
    }

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
        throw new UnsupportedOperationException();
    }
}
