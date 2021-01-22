package appbox.data;

import appbox.model.EntityModel;
import appbox.runtime.RuntimeContext;
import appbox.serialization.*;

public abstract class Entity implements IBinSerializable {

    private long _modelId;

    public Entity(long modelId) {
        _modelId = modelId;
    }

    public final long modelId() {
        return _modelId;
    }

    public final EntityModel model() {
        return RuntimeContext.current().getModel(_modelId);
    }

    //region ====Serialization====
    @Override
    public void writeTo(IOutputStream bs) {
        //先加入已序列化列表
        bs.addToSerialized(this);

        bs.writeLong(_modelId);

        //write members
        var model = model(); //TODO:考虑判断model.schemaVersion与entity是否一致
        for (var m : model.getMembers()) {
            writeMember(m.memberId(), bs, IEntityMemberWriter.SF_NONE);
        }
        bs.writeShort((short) 0); //end write members
    }

    @Override
    public void readFrom(IInputStream bs) {
        _modelId = bs.readLong();

        //read members
        short memberId;
        while (true) {
            memberId = bs.readShort();
            if (memberId == 0)
                break;
            readMember(memberId, bs, IEntityMemberReader.SF_NONE);
        }
    }

    /**
     * 写入成员至IEntityMemberWriter，由IEntityMemberWriter及flags决定写入格式
     * @param flags 写入目标格式标记
     */
    public abstract void writeMember(short id, IEntityMemberWriter bs, byte flags);

    /**
     * 从IEntityMemberReader读取指定成员Id的成员值
     * @param flags 读取格式标记
     */
    public abstract void readMember(short id, IEntityMemberReader bs, int flags);
    //endregion
}
