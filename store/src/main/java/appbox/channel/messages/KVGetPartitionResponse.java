package appbox.channel.messages;

import appbox.serialization.IInputStream;

public final class KVGetPartitionResponse extends KVGetResponse {
    public long raftGroupId;

    @Override
    public void readFrom(IInputStream bs) {
        reqId     = bs.readInt();
        errorCode = bs.readInt();
        checkStoreError();

        var size = bs.readNativeVariant(); //跳过长度
        if (size > 0) {
            raftGroupId = bs.readLong();
        }
    }
}
