package appbox.data;

import appbox.model.EntityModel;
import appbox.runtime.RuntimeContext;
import appbox.serialization.*;

public abstract class Entity implements IBinSerializable {

    /** 实体对应的模型标识 */
    public abstract long modelId();

    public final EntityModel model() {
        return RuntimeContext.current().getModel(modelId());
    }

    //region ====Serialization====
    @Override
    public void writeTo(IOutputStream bs) {
        //先加入已序列化列表
        bs.addToSerialized(this);

        //write members
        final var model = model(); //TODO:考虑判断model.schemaVersion与entity是否一致
        for (var m : model.getMembers()) {
            writeMember(m.memberId(), bs, IEntityMemberWriter.SF_NONE);
        }
        bs.writeShort((short) 0); //end write members
    }

    @Override
    public void readFrom(IInputStream bs) {
        //先加入已反序列化列表
        bs.addToDeserialized(this);

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
