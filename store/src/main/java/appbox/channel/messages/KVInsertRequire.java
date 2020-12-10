package appbox.channel.messages;

import appbox.channel.IMessage;
import appbox.channel.MessageType;
import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;
import appbox.store.KVTxnId;

public abstract class KVInsertRequire implements IMessage {
    private final KVTxnId txnId = new KVTxnId();
    protected     long    raftGroupId;
    protected     int     schemaVersion;
    protected     byte    dataCF;
    public        boolean overrideExists;

    public KVInsertRequire(KVTxnId txnId) {
        this.txnId.copyFrom(txnId);
    }

    @Override
    public final byte MessageType() {
        return MessageType.KVInsertRequire;
    }

    //region ====Serialization====
    @Override
    public void writeTo(IOutputStream bs) {
        txnId.writeTo(bs);

        bs.writeLong(raftGroupId);
        bs.writeInt(schemaVersion);
        bs.writeByte(dataCF);
        bs.writeBool(overrideExists);
    }

    @Override
    public final void readFrom(IInputStream bs) {
        throw new UnsupportedOperationException();
    }
    //endregion
}
