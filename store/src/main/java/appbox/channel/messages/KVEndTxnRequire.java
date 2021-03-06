package appbox.channel.messages;

import appbox.channel.IMessage;
import appbox.channel.MessageType;
import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;
import appbox.store.KVTxnId;

public final class KVEndTxnRequire implements IMessage {
    public final KVTxnId txnId = new KVTxnId();
    public       byte    action; //0=Commit,1=Rollback,2=Abort

    @Override
    public byte MessageType() {
        return MessageType.KVEndTxnRequire;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        txnId.writeTo(bs);
        bs.writeByte(action);
    }

    @Override
    public void readFrom(IInputStream bs) {
        throw new UnsupportedOperationException();
    }
}
