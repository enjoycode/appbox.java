package appbox.data;

import appbox.serialization.IEntityMemberReader;

public abstract class SysIndex<T extends SysEntity> implements IKVRow {

    private EntityId _targetId;

    public EntityId getTargetId() { return _targetId; }

    /** 仅用于内部从存储读取 */
    public void setTargetId(EntityId targetId) {
        _targetId = targetId;
    }

    public abstract void readMember(short id, IEntityMemberReader bs, int flags);

}
