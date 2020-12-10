package appbox.channel.messages;

import appbox.channel.MessageType;
import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;
import appbox.store.KVTxnId;

public final class KVBeginTxnResponse extends StoreResponse {
    //TODO:ObjectPool

    public final KVTxnId txnId = new KVTxnId();

    @Override
    public byte MessageType() {
        return MessageType.KVBeginTxnResponse;
    }

    @Override
    public void writeTo(IOutputStream bs) {

    }

    @Override
    public void readFrom(IInputStream bs) {
        reqId = bs.readInt();
        errorCode = bs.readInt();
        txnId.readFrom(bs);
    }
}
