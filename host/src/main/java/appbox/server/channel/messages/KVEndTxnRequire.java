package appbox.server.channel.messages;

import appbox.core.serialization.BinDeserializer;
import appbox.core.serialization.BinSerializer;
import appbox.server.channel.MessageType;
import appbox.store.KVTxnId;

public final class KVEndTxnRequire implements IMessage {
    public       int     reqId;
    public       byte    action; //0=Commit,1=Rollback,2=Abort
    public final KVTxnId txnId = new KVTxnId();

    @Override
    public byte MessageType() {
        return MessageType.KVEndTxnRequire;
    }

    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        bs.writeInt(reqId);
        bs.writeByte(action);
        txnId.writeTo(bs);
    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {

    }
}
