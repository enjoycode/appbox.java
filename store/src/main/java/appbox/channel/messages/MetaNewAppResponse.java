package appbox.channel.messages;

import appbox.channel.MessageType;
import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;

public final class MetaNewAppResponse extends StoreResponse {
    public byte appId;

    @Override
    public byte MessageType() {
        return MessageType.MetaNewAppResponse;
    }

    //region ====Serialization====
    @Override
    public void writeTo(IOutputStream bs) {

    }

    @Override
    public void readFrom(IInputStream bs) {
        reqId     = bs.readInt();
        errorCode = bs.readInt();
        appId     = bs.readByte();
    }
    //endregion
}
