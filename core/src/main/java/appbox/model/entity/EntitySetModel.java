package appbox.model.entity;

import appbox.model.EntityModel;
import appbox.serialization.IInputStream;
import appbox.serialization.IJsonWriter;
import appbox.serialization.IOutputStream;

public final class EntitySetModel extends EntityMemberModel {

    private long  _refModelId; //引用的实体模型标识号，如Order->OrderDetail，则指向OrderDetail的模型标识
    private short _refMemberId; //引用的EntityRef成员标识，如Order->OrderDetail，则指向OrderDetail.Order成员标识

    /** Only for serialization */
    public EntitySetModel(EntityModel owner) {
        super(owner);
        _allowNull = true;
    }

    /** 设计时新建EntitySet成员 */
    public EntitySetModel(EntityModel owner, String name, long refModelId, short refMemberId) {
        super(owner, name, true);

        this._refModelId  = refModelId;
        this._refMemberId = refMemberId;
    }

    //region ====Properties====
    @Override
    public EntityMemberType type() { return EntityMemberType.EntitySet; }

    public long refModelId() { return _refModelId; }

    public short refMemberId() { return _refMemberId; }
    //endregion

    //region ====Design Methods====
    @Override
    public void setAllowNull(boolean value) { /*do nothing, always allow null. */}
    //endregion

    //region ====Serialization====
    @Override
    public void writeTo(IOutputStream bs) {
        super.writeTo(bs);

        bs.writeLongField(_refModelId, 1);
        bs.writeShortField(_refMemberId, 2);

        bs.finishWriteFields();
    }

    @Override
    public void readFrom(IInputStream bs) {
        super.readFrom(bs);

        int propIndex;
        do {
            propIndex = bs.readVariant();
            switch (propIndex) {
                case 1:
                    _refModelId = bs.readLong(); break;
                case 2:
                    _refMemberId = bs.readShort(); break;
                case 0:
                    break;
                default:
                    throw new RuntimeException("Unknown field id: " + propIndex);
            }
        } while (propIndex != 0);
    }

    @Override
    protected void writeJsonMembers(IJsonWriter writer) {
        writer.writeKey("RefModelId");
        writer.writeValue(Long.toUnsignedString(_refModelId));

        writer.writeKey("RefMemberId");
        writer.writeValue(_refMemberId);
    }
    //endregion

}
