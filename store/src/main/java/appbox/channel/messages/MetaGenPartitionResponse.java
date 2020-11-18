package appbox.channel.messages;

import appbox.channel.MessageType;
import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;

public final class MetaGenPartitionResponse extends StoreResponse {
    public long raftGroupId;

    @Override
    public byte MessageType() {
        return MessageType.MetaGenPartitionResponse;
    }

    @Override
    public void writeTo(BinSerializer bs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readFrom(BinDeserializer bs) {
        reqId       = bs.readInt();
        errorCode   = bs.readInt();
        if (errorCode == 0) {
            raftGroupId = bs.readLong();
        }
    }
}
