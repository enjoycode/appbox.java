package appbox.channel.messages;

import appbox.channel.MessageType;
import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;

public final class MetaGenPartitionResponse extends StoreResponse {
    public long raftGroupId;

    @Override
    public byte MessageType() {
        return MessageType.MetaGenPartitionResponse;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readFrom(IInputStream bs) {
        reqId       = bs.readInt();
        errorCode   = bs.readInt();
        if (errorCode == 0) {
            raftGroupId = bs.readLong();
        }
    }
}
