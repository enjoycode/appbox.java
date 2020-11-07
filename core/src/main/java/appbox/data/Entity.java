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
    public final void writeTo(BinSerializer bs) throws Exception {
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
    }

    @Override
    public final void readFrom(BinDeserializer bs) throws Exception {
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
    }

    /**
     * 写入成员Id及对应的值
     * @param storeFlags 写入到系统存储的格式
     */
    public abstract void writeMember(short id, IEntityMemberWriter bs, byte storeFlags) throws Exception;

    /**
     * 读取指定成员Id的成员值
     * @param storeFlags 从系统存储读的格式
     */
    public abstract void readMember(short id, IEntityMemberReader bs, int storeFlags) throws Exception;
    //endregion
}
