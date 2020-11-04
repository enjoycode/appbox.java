package appbox.channel.messages;

import appbox.serialization.BinSerializer;
import appbox.store.KeyUtil;
import appbox.store.PartitionInfo;

public final class KVGetPartitionRequest extends KVGetRequest {

    private final PartitionInfo partitionInfo;

    public KVGetPartitionRequest(PartitionInfo partitionInfo) {
        this.partitionInfo = partitionInfo;
    }

    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        bs.writeInt(0); //ReqId占位
        bs.writeLong(KeyUtil.META_RAFTGROUP_ID); //raftGroupId
        bs.writeNativeVariant(partitionInfo.key.length); //key长度
        bs.write(partitionInfo.key); //key数据
        bs.writeByte(KeyUtil.PARTCF_INDEX);    //dataCF
        bs.writeByte(KVReadDataType.Partition.value);  //dataType
        bs.writeLong(0);      //timestamp
    }

}
