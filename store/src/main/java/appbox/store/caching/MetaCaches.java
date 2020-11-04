package appbox.store.caching;

import appbox.store.PartitionInfo;

import java.util.concurrent.ConcurrentHashMap;

public final class MetaCaches {

    //TODO: use LRUCache

    /** 分区信息，Key=分区键 Value=RaftGroupId */
    private static final ConcurrentHashMap<byte[], Long> partitions = new ConcurrentHashMap<>(100);

    public static Long tryGetPartition(PartitionInfo partitionInfo) {
        return partitions.get(partitionInfo.key);
    }

    public static void tryAddPartition(PartitionInfo partitionInfo, Long raftGroupId) {
        partitions.putIfAbsent(partitionInfo.key, raftGroupId);
    }

}
