package appbox.model;

import appbox.model.entity.DataFieldModel;
import appbox.model.entity.EntityMemberModel;
import appbox.serialization.IBinSerializable;
import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;

/** 专用于发送成员映射关系给前端序列化 */
public final class EntityModelInfo implements IBinSerializable {
    private final EntityModel _model;

    public EntityModelInfo(EntityModel model) {
        _model = model;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        //写入映射的存储类型0=DTO 1=Sys 2=Sql
        byte storeType = 0;
        if (_model.sysStoreOptions() != null)
            storeType = 1;
        else if (_model.sqlStoreOptions() != null)
            storeType = 2;
        bs.writeByte(storeType);

        //暂不写入成员数量,因为前端只会单个获取模型信息
        for (var m : _model.getMembers()) {
            bs.writeShort(m.memberId());
            //写入成员类型
            bs.writeByte(m.type().value);
            //如果是DataField写入字段类型
            if (m.type() == EntityMemberModel.EntityMemberType.DataField) {
                bs.writeByte(((DataFieldModel)m).dataType().value);
            } else {
                bs.writeByte((byte)0);
            }
            bs.writeString(m.name());
        }
    }

    @Override
    public void readFrom(IInputStream bs) {
        throw new UnsupportedOperationException();
    }
}
