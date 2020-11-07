package appbox.channel.messages;

import appbox.channel.MessageType;
import appbox.model.ApplicationModel;
import appbox.model.ModelBase;
import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;

public abstract class KVScanResponse extends StoreResponse {

    public int    skipped;
    public int    length;

    @Override
    public byte MessageType() {
        return MessageType.KVScanResponse;
    }

    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        throw new UnsupportedOperationException();
    }

}
