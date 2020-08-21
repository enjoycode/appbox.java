package appbox.model.entity;

import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;

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

        public final byte value;

        PartitionKeyRule(byte value) {
            this.value = value;
        }

        public static PartitionKeyRule fromValue(byte v) throws Exception {
            switch (v) {
                case 0:
                    return None;
                case 1:
                    return Hash;
                case 2:
                    return RangeOfDate;
                default:
                    throw new Exception("Unknown value");
            }
        }
    }

    public enum DatePeriod {
        Year((byte) 0), Month((byte) 1), Day((byte) 2);

        public final byte value;

        DatePeriod(byte value) {
            this.value = value;
        }
    }

    /**
     * 分区键实体成员标识，0特殊表示默认的创建时间
     */
    public short            memberId;
    public boolean          orderByDesc;
    public PartitionKeyRule rule;
    public int              ruleArgument;

    //region ====Serialization====
    public void writeTo(BinSerializer bs) throws Exception {
        bs.writeShort(memberId);
        bs.writeBool(orderByDesc);
        bs.writeByte(rule.value);
        bs.writeVariant(ruleArgument);
    }

    public void readFrom(BinDeserializer bs) throws Exception {
        memberId     = bs.readShort();
        orderByDesc  = bs.readBool();
        rule         = PartitionKeyRule.fromValue(bs.readByte());
        ruleArgument = bs.readVariant();
    }
    //endregion
}
