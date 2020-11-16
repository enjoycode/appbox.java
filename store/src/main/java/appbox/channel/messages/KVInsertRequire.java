package appbox.channel.messages;

import appbox.channel.IMessage;
import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.channel.MessageType;
import appbox.store.KVTxnId;

import javax.naming.OperationNotSupportedException;

public abstract class KVInsertRequire implements IMessage {
    private final KVTxnId txnId = new KVTxnId();
    public        long    raftGroupId;
    public        int     schemaVersion;
    protected     byte    dataCF;
    public        boolean overrideExists;

    public KVInsertRequire(KVTxnId txnId) {
        this.txnId.copyFrom(txnId);
    }

    @Override
    public byte MessageType() {
        return MessageType.KVInsertRequire;
    }

    //region ====Serialization====
    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        txnId.writeTo(bs);

        bs.writeLong(raftGroupId);
        bs.writeInt(schemaVersion);
        bs.writeByte(dataCF);
        bs.writeBool(overrideExists);
    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {
        throw new OperationNotSupportedException();
    }
    //endregion
}
