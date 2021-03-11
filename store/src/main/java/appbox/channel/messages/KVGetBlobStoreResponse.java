package appbox.channel.messages;

import appbox.serialization.IInputStream;

public class KVGetBlobStoreResponse extends KVGetResponse {
    public byte blobStoreId;
    public int  maxChunkSize;

    @Override
    public void readFrom(IInputStream bs) {
        reqId     = bs.readInt();
        errorCode = bs.readInt();
        checkStoreError();

        var size = bs.readNativeVariant(); //跳过长度
        if (size > 0) {
            blobStoreId  = bs.readByte();
            maxChunkSize = bs.readInt();
        }
    }
}
