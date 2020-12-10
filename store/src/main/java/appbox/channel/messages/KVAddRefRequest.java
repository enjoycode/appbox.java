package appbox.channel.messages;

import appbox.channel.IMessage;
import appbox.channel.MessageType;
import appbox.data.EntityId;
import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.serialization.IOutputStream;
import appbox.store.KVTxnId;

public final class KVAddRefRequest implements IMessage {

    private final KVTxnId  _txnId = new KVTxnId();
    public final EntityId targetEntityId;
    public final long    fromRaftGroupId;
    private final int     _fromTableId;
    private int      _diff;

    public KVAddRefRequest(KVTxnId txnId, EntityId targetEntityId,
                           long fromRaftGroupId, int fromTableId, int diff) {
        _txnId.copyFrom(txnId);
        this.targetEntityId  = targetEntityId;
        this.fromRaftGroupId = fromRaftGroupId;
        _fromTableId         = fromTableId;
        _diff              = diff;
    }

    public int getDiff() { return _diff; }

    public void addDiff(int diff) {
        _diff += diff;
    }

    @Override
    public byte MessageType() { return MessageType.KVAddRefRequest;}

    @Override
    public void writeTo(IOutputStream bs) {
        _txnId.writeTo(bs);
        bs.writeLong(targetEntityId.raftGroupId()); //TODO:暂为了兼容存储层
        bs.writeLong(fromRaftGroupId);
        bs.writeInt(_fromTableId);
        bs.writeInt(_diff);
        targetEntityId.writeTo(bs);
    }

    @Override
    public void readFrom(BinDeserializer bs) {
        throw new UnsupportedOperationException();
    }
}
