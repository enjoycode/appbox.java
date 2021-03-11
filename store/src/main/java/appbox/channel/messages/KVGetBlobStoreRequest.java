package appbox.channel.messages;

import appbox.serialization.IOutputStream;
import appbox.store.KVUtil;

/** 根据名称获取系统BlobStore的Id */
public class KVGetBlobStoreRequest extends KVGetRequest {

    private final String storeName;

    public KVGetBlobStoreRequest(String storeName) {
        this.storeName = storeName;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeInt(0); //ReqId占位
        bs.writeLong(KVUtil.META_RAFTGROUP_ID); //raftGroupId
        bs.writeByte((byte) -1);    //dataCF
        bs.writeLong(0);      //timestamp
        KVUtil.writeBlobStoreKey(bs, storeName); //key
    }

}
