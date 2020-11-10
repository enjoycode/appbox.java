package appbox.store.query;

import appbox.expressions.Expression;

/**
 * 作为TableScan及IndexScan的基类
 */
public abstract class KVScan {
    protected final long modelId;
    protected       int  skip;
    protected       int  take = Integer.MAX_VALUE; //MaxTake;

    /// <summary>
    /// 记录过滤条件表达式
    /// </summary>
    protected Expression filter;

    protected KVScan(long modelId) {
        this.modelId = modelId;
    }

    /// <summary>
    /// 用于EagerLoad导航属性
    /// </summary>
    //protected Includer rootIncluder;

    //protected PartitionPredicates partitions;
    /// <summary>
    /// 用于分区表指定分区的谓词
    /// </summary>
    //public PartitionPredicates getPartitions() {
    //    if (partitions == null)
    //        partitions = new PartitionPredicates();
    //    return partitions;
    //}

}
