package appbox.channel.messages;

import appbox.channel.IMessage;
import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.channel.MessageType;
import appbox.store.KVTxnId;

public abstract class KVDeleteRequest implements IMessage {

    protected final KVTxnId txnId         = new KVTxnId();
    protected       long    raftGroupId   = 0;
    protected       int     schemaVersion = 0;
    protected       byte    dataCF        = -1;
    protected       boolean returnExists  = false;

    public KVDeleteRequest(KVTxnId txnId) {
        this.txnId.copyFrom(txnId);
    }

    @Override
    public byte MessageType() {
        return MessageType.KVDeleteRequire;
    }

    //region ====Serialization====
    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        txnId.writeTo(bs);

        bs.writeLong(raftGroupId);
        bs.writeInt(schemaVersion);
        bs.writeByte(dataCF);
        bs.writeBool(returnExists);

        //子类写入refs及不带长度信息的Key
    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {
        throw new UnsupportedOperationException();
    }
    //endregion
}
