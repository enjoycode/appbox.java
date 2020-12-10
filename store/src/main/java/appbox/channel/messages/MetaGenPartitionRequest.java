package appbox.channel.messages;

import appbox.channel.IMessage;
import appbox.channel.MessageType;
import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.serialization.IOutputStream;
import appbox.store.KVTxnId;
import appbox.store.PartitionInfo;

public final class MetaGenPartitionRequest implements IMessage {
    private final PartitionInfo partitionInfo;
    private final KVTxnId txnId;

    public MetaGenPartitionRequest(PartitionInfo partitionInfo, KVTxnId txnId) {
        this.partitionInfo = partitionInfo;
        this.txnId = new KVTxnId();
        this.txnId.copyFrom(txnId);
    }

    @Override
    public byte MessageType() {
        return MessageType.MetaGenPartitionRequest;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        //txnId
        txnId.writeTo(bs);
        //raftType flags
        bs.writeByte(partitionInfo.flags);
        //partcf key, 不需要写入长度
        bs.write(partitionInfo.key, 0, partitionInfo.key.length);
    }

    @Override
    public void readFrom(BinDeserializer bs) {
        throw new UnsupportedOperationException();
    }
}
