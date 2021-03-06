package appbox.channel.messages;

import appbox.channel.IMessage;
import appbox.channel.MessageType;
import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;
import appbox.store.KVTxnId;

public abstract class KVUpdateRequest implements IMessage {

    protected final KVTxnId txnId         = new KVTxnId();
    protected       long    raftGroupId   = 0;
    protected       int     schemaVersion = 0;
    protected       byte    dataCF        = -1;
    protected       boolean merge         = false;
    protected       boolean returnExists  = false;

    public KVUpdateRequest(KVTxnId txnId) {
        this.txnId.copyFrom(txnId);
    }

    @Override
    public final byte MessageType() {
        return MessageType.KVUpdateRequire;
    }

    //region ====Serialization====
    @Override
    public void writeTo(IOutputStream bs) {
        txnId.writeTo(bs);

        bs.writeLong(raftGroupId);
        bs.writeInt(schemaVersion);
        bs.writeByte(dataCF);
        bs.writeBool(merge);
        bs.writeBool(returnExists);

        //子类写入key(带长度),refs及不带长度信息的data
    }

    @Override
    public final void readFrom(IInputStream bs) {
        throw new UnsupportedOperationException();
    }
    //endregion

}
