package appbox.server.channel.messages;

import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.server.channel.MessageType;
import appbox.store.KVTxnId;

public final class KVBeginTxnResponse extends StoreResponse {
    //TODO:ObjectPool

    public       int     errorCode;
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
