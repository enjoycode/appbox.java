package appbox.channel.messages;

import appbox.serialization.BinDeserializer;

public final class KVGetPartitionResponse extends KVGetResponse {
    public long raftGroupId;

    @Override
    public void readFrom(BinDeserializer bs) {
        reqId     = bs.readInt();
        errorCode = bs.readInt();

        if (errorCode == 0) {
            var size = bs.readNativeVariant(); //跳过长度
            if (size > 0) {
                raftGroupId = bs.readLong();
            }
        }
    }
}
