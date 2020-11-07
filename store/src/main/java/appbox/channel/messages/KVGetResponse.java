package appbox.channel.messages;

import appbox.channel.MessageType;
import appbox.serialization.BinSerializer;

public abstract class KVGetResponse extends StoreResponse {

    @Override
    public byte MessageType() {
        return MessageType.KVGetResponse;
    }

    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        throw new RuntimeException("Not supported.");
    }

}
