package appbox.channel.messages;

import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.channel.MessageType;
import appbox.store.KVTxnId;

public final class KVBeginTxnResponse extends StoreResponse {
    //TODO:ObjectPool

    public final KVTxnId txnId = new KVTxnId();

    @Override
    public byte MessageType() {
        return MessageType.KVBeginTxnResponse;
    }

    @Override
    public void writeTo(BinSerializer bs) throws Exception {

    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {
        reqId = bs.readInt();
        errorCode = bs.readInt();
        txnId.readFrom(bs);
    }
}
