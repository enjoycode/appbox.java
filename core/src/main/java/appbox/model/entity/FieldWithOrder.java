package appbox.model.entity;

import appbox.serialization.*;

/**
 * 带排序标记的字段
 */
public final class FieldWithOrder implements IBinSerializable, IJsonSerializable { //TODO: rename to OrderedField

    public short   memberId;
    public boolean orderByDesc;

    FieldWithOrder() {}

    public FieldWithOrder(short memberId) {
        this.memberId    = memberId;
        this.orderByDesc = false;
    }

    public FieldWithOrder(short memberId, boolean orderByDesc) {
        this.memberId    = memberId;
        this.orderByDesc = orderByDesc;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeShort(memberId);
        bs.writeBool(orderByDesc);
    }

    @Override
    public void readFrom(IInputStream bs) {
        memberId    = bs.readShort();
        orderByDesc = bs.readBool();
    }

    @Override
    public void writeToJson(IJsonWriter writer) {
        writer.startObject();
        writer.writeKeyValue("MemberId", memberId);
        writer.writeKeyValue("OrderByDesc", orderByDesc);
        writer.endObject();
    }
}
