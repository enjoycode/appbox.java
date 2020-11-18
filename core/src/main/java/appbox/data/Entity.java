package appbox.data;

import appbox.model.EntityModel;
import appbox.runtime.RuntimeContext;
import appbox.serialization.*;

public abstract class Entity implements IBinSerializable {

    private long _modelId;

    public Entity(long modelId) {
        _modelId = modelId;
    }

    public EntityModel model() {
        return RuntimeContext.current().getModel(_modelId);
    }

    protected void onPropertyChanged(short memberId) {
        //TODO:
    }

    //region ====Serialization====
    @Override
    public final void writeTo(BinSerializer bs) {
        bs.writeLong(_modelId);

        if (this instanceof SysEntity) {
            ((SysEntity) this).id().writeTo(bs);
        }

        //write members
        var model = model(); //TODO:考虑判断model.schemaVersion与entity是否一致
        for (var m : model.getMembers()) {
            writeMember(m.memberId(), bs, IEntityMemberWriter.SF_NONE);
        }
        bs.writeShort((short) 0); //end write members

        //TODO:写入扩展信息
    }

    @Override
    public final void readFrom(BinDeserializer bs) {
        _modelId = bs.readLong();

        if (this instanceof SysEntity) {
            ((SysEntity) this).id().readFrom(bs);
        }

        //read members
        short memberId;
        while (true) {
            memberId = bs.readShort();
            if (memberId == 0) {
                break;
            }
            readMember(memberId, bs, IEntityMemberReader.SF_NONE);
        }

        //TODO:读取扩展信息
    }

    /**
     * 写入成员，由IEntityMemberWriter及flags决定写入格式
     * @param flags 写入目标格式标记
     */
    public abstract void writeMember(short id, IEntityMemberWriter bs, byte flags);

    /**
     * 读取指定成员Id的成员值
     * @param flags 读取格式标记
     */
    public abstract void readMember(short id, IEntityMemberReader bs, int flags);
    //endregion
}
