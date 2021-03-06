package appbox.channel.messages;

import appbox.serialization.IOutputStream;
import appbox.store.KVUtil;
import appbox.store.PartitionInfo;

public final class KVGetPartitionRequest extends KVGetRequest {

    private final PartitionInfo partitionInfo;

    public KVGetPartitionRequest(PartitionInfo partitionInfo) {
        this.partitionInfo = partitionInfo;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeInt(0); //ReqId占位
        bs.writeLong(KVUtil.META_RAFTGROUP_ID); //raftGroupId
        bs.writeByte(KVUtil.PARTCF_INDEX);    //dataCF
        bs.writeLong(0);      //timestamp
        bs.write(partitionInfo.key, 0, partitionInfo.key.length); //key
    }

}
