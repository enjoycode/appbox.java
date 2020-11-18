package appbox.channel.messages;

import appbox.channel.IMessage;
import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.channel.MessageType;
import appbox.store.KVTxnId;

public final class KVEndTxnRequire implements IMessage {
    public final KVTxnId txnId = new KVTxnId();
    public       byte    action; //0=Commit,1=Rollback,2=Abort

    @Override
    public byte MessageType() {
        return MessageType.KVEndTxnRequire;
    }

    @Override
    public void writeTo(BinSerializer bs) {
        txnId.writeTo(bs);
        bs.writeByte(action);
    }

    @Override
    public void readFrom(BinDeserializer bs) {
        throw new UnsupportedOperationException();
    }
}
