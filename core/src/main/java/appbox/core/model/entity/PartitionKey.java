package appbox.core.model.entity;

/**
 * 系统存储的分区键
 */
public final class PartitionKey {
    public enum PartitionKeyRule {
        /**
         * 按指定成员值
         */
        None((byte) 0),
        /**
         * 按Hash
         */
        Hash((byte) 1),
        /**
         * 按时间区间
         */
        RangeOfDate((byte) 2);

        private byte _value;

        PartitionKeyRule(byte value) {
            _value = value;
        }
    }

    public enum DatePeriod {
        Year((byte) 0), Month((byte) 1), Day((byte) 2);

        private byte _value;

        DatePeriod(byte value) {
            _value = value;
        }
    }

    /**
     * 分区键实体成员标识，0特殊表示默认的创建时间
     */
    public short            memberId;
    public boolean          orderByDesc;
    public PartitionKeyRule rule;
    public int              ruleArgument;
}
