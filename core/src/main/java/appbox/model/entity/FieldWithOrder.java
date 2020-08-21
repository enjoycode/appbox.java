package appbox.model.entity;

import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;

/**
 * 带排序标记的字段
 */
public final class FieldWithOrder {

    public short   memberId;
    public boolean orderByDesc;

    FieldWithOrder() {
    }

    public FieldWithOrder(short memberId) {
        this.memberId    = memberId;
        this.orderByDesc = false;
    }

    public FieldWithOrder(short memberId, boolean orderByDesc) {
        this.memberId    = memberId;
        this.orderByDesc = orderByDesc;
    }

    public void writeTo(BinSerializer bs) throws Exception {
        bs.writeShort(memberId);
        bs.writeBool(orderByDesc);
    }

    public void readFrom(BinDeserializer bs) throws Exception {
        memberId    = bs.readShort();
        orderByDesc = bs.readBool();
    }
}
