package appbox.channel.messages;

import appbox.channel.MessageType;
import appbox.serialization.IOutputStream;

public abstract class KVGetResponse extends StoreResponse {

    @Override
    public byte MessageType() {
        return MessageType.KVGetResponse;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        throw new UnsupportedOperationException();
    }

}
