package appbox.channel.messages;

import appbox.channel.MessageType;
import appbox.serialization.BinSerializer;

public abstract class KVScanResponse extends StoreResponse {

    public int    skipped;
    public int    length;

    @Override
    public byte MessageType() {
        return MessageType.KVScanResponse;
    }

    @Override
    public void writeTo(BinSerializer bs) {
        throw new UnsupportedOperationException();
    }

}
