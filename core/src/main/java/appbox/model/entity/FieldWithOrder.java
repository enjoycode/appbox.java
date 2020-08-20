package appbox.model.entity;

/**
 * 带排序标记的字段
 */
public final class FieldWithOrder {

    public final short   memberId;
    public final boolean orderByDesc;

    public FieldWithOrder(short memberId) {
        this.memberId    = memberId;
        this.orderByDesc = false;
    }

    public FieldWithOrder(short memberId, boolean orderByDesc) {
        this.memberId    = memberId;
        this.orderByDesc = orderByDesc;
    }
}
