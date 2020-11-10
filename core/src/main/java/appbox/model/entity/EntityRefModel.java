package appbox.model.entity;

import appbox.model.EntityModel;
import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;

import java.util.ArrayList;
import java.util.List;

public final class EntityRefModel extends EntityMemberModel {
    public enum EntityRefActionRule {
        NoAction(0),
        Cascade(1),
        SetNull(2);

        public final byte value;

        EntityRefActionRule(int value) {
            this.value = (byte) value;
        }

        public static EntityRefModel.EntityRefActionRule fromValue(byte v) throws Exception {
            for (EntityRefModel.EntityRefActionRule item : EntityRefModel.EntityRefActionRule.values()) {
                if (item.value == v) {
                    return item;
                }
            }
            throw new RuntimeException("Unknown value: " + v);
        }
    }

    private       boolean    isReverse; //是否反向引用 eg: A->B , B->A(反向)
    private       boolean    isForeignKeyConstraint; //是否强制外键约束
    private final List<Long> refModelIds; //引用的实体模型标识号集合，聚合引用有多个
    /**
     * 引用的外键成员标识集合
     * 1. SysStore只有一个Id, eg: Order->Customer为Order.CustomerId
     * 2. SqlStore有一或多个，与引用目标的主键的数量、顺序、类型一致
     */
    private       short[]    fkMemberIds;
    private       short      typeMemberId; //聚合引用时的类型字段，存储引用目标的EntityModel.Id

    private EntityRefActionRule updateRule = EntityRefActionRule.Cascade;
    private EntityRefActionRule deleteRule = EntityRefActionRule.NoAction;

    /** Only for serialization */
    public EntityRefModel(EntityModel owner) {
        super(owner);
        refModelIds = new ArrayList<>();
    }

    /** 设计时新建非聚合引用成员 */
    public EntityRefModel(EntityModel owner, String name, long refModelId,
                          short[] fkMemberIds, boolean foreignConstraint) {
        super(owner, name, true);

        if (fkMemberIds == null || fkMemberIds.length == 0) {
            throw new IllegalArgumentException();
        }

        refModelIds            = new ArrayList<>() {{ add(refModelId);}};
        isReverse              = false;
        this.fkMemberIds       = fkMemberIds;
        typeMemberId           = 0;
        isForeignKeyConstraint = foreignConstraint;
    }

    /** 设计时新建非聚合引用成员 */
    public EntityRefModel(EntityModel owner, String name, List<Long> refModelIds,
                          short[] fkMemberIds, short typeMemberId, boolean foreignConstraint) {
        super(owner, name, true);

        if (fkMemberIds == null || fkMemberIds.length == 0) {
            throw new IllegalArgumentException();
        }
        if (refModelIds == null || refModelIds.size() == 0) {
            throw new IllegalArgumentException();
        }

        this.refModelIds       = refModelIds;
        isReverse              = false;
        this.fkMemberIds       = fkMemberIds;
        this.typeMemberId      = typeMemberId;
        isForeignKeyConstraint = foreignConstraint;
    }

    //region ====Properties====

    /** 是否聚合引用至不同的实体模型 */
    public boolean isAggregationRef() {
        return typeMemberId != 0;
    }

    @Override
    public EntityMemberType type() {
        return EntityMemberType.EntityRef;
    }
    //endregion

    //region ====Design Methods====
    @Override
    public void setAllowNull(boolean value) {
        _allowNull = value;
        for (var fkid : fkMemberIds) {
            owner.getMember(fkid).setAllowNull(value);
        }
        if (isAggregationRef()) {
            owner.getMember(typeMemberId).setAllowNull(value);
        }
    }
    //endregion

    //region ====Serialization====
    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        super.writeTo(bs);

        bs.writeBool(isReverse, 1);
        bs.writeBool(isForeignKeyConstraint, 4);
        bs.writeShort(typeMemberId, 6);
        bs.writeByte(updateRule.value, 7);
        bs.writeByte(deleteRule.value, 8);

        bs.writeVariant(3);
        bs.writeVariant(refModelIds.size());
        for (Long refModelId : refModelIds) {
            bs.writeLong(refModelId);
        }

        bs.writeVariant(5);
        bs.writeVariant(fkMemberIds.length);
        for (short fkMemberId : fkMemberIds) {
            bs.writeShort(fkMemberId);
        }

        bs.finishWriteFields();
    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {
        super.readFrom(bs);

        int propIndex;
        do {
            propIndex = bs.readVariant();
            switch (propIndex) {
                case 1:
                    isReverse = bs.readBool(); break;
                case 4:
                    isForeignKeyConstraint = bs.readBool(); break;
                case 6:
                    typeMemberId = bs.readShort(); break;
                case 7:
                    updateRule = EntityRefActionRule.fromValue(bs.readByte()); break;
                case 8:
                    deleteRule = EntityRefActionRule.fromValue(bs.readByte()); break;
                case 3: {
                    var count = bs.readVariant();
                    for (int i = 0; i < count; i++) {
                        refModelIds.add(bs.readLong());
                    }
                } break;
                case 5: {
                    var count = bs.readVariant();
                    fkMemberIds = new short[count];
                    for (int i = 0; i < count; i++) {
                        fkMemberIds[i] = bs.readShort();
                    }
                } break;
                case 0:
                    break;
                default:
                    throw new RuntimeException("Unknown field id: " + propIndex);
            }
        } while (propIndex != 0);
    }
    //endregion

    public List<Long> getRefModelIds() {
        return refModelIds;
    }
}
