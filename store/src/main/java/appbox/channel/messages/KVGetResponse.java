package appbox.channel.messages;

import appbox.channel.MessageType;
import appbox.serialization.BinSerializer;

public abstract class KVGetResponse extends StoreResponse {

    @Override
    public byte MessageType() {
        return MessageType.KVGetResponse;
    }

    @Override
    public void writeTo(BinSerializer bs) {
        throw new UnsupportedOperationException();
    }

}
