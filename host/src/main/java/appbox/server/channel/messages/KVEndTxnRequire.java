package appbox.server.channel.messages;

import appbox.core.serialization.BinDeserializer;
import appbox.core.serialization.BinSerializer;
import appbox.server.channel.MessageType;
import appbox.store.KVTxnId;

public final class KVEndTxnRequire implements IMessage {
    public final KVTxnId txnId = new KVTxnId();
    public       byte    action; //0=Commit,1=Rollback,2=Abort

    @Override
    public byte MessageType() {
        return MessageType.KVEndTxnRequire;
    }

    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        txnId.writeTo(bs);
        bs.writeByte(action);
    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {

    }
}
