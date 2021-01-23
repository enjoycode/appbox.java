package appbox.channel.messages;

import appbox.data.SysEntity;
import appbox.model.EntityModel;
import appbox.model.entity.EntityMemberModel;
import appbox.model.entity.EntityRefModel;
import appbox.serialization.IEntityMemberWriter;
import appbox.serialization.IOutputStream;
import appbox.store.KVTxnId;
import appbox.store.KVUtil;

import java.util.List;

public final class KVUpdateEntityRequest extends KVUpdateRequest {

    private final SysEntity            _entity;
    private final EntityModel          _model;
    private final List<EntityRefModel> _refsWithFK;

    public KVUpdateEntityRequest(SysEntity entity, EntityModel model,
                                 List<EntityRefModel> refsWithFK, KVTxnId txnId) {
        super(txnId);

        dataCF        = -1;
        raftGroupId   = entity.id().raftGroupId();
        schemaVersion = model.sysStoreOptions().schemaVersion();
        merge         = false;
        returnExists  = model.sysStoreOptions().hasIndexes() || refsWithFK != null;

        _entity     = entity;
        _model      = model;
        _refsWithFK = refsWithFK;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        super.writeTo(bs);

        //key
        KVUtil.writeEntityKey(bs, _entity.id(), true);
        //refs
        KVUtil.writeEntityRefs(bs, _refsWithFK);
        //data //TODO:根据是否merge写入仅变更的成员
        for (var m : _model.getMembers()) {
            if (m.type() == EntityMemberModel.EntityMemberType.DataField) {
                _entity.writeMember(m.memberId(), bs, /*注意更新需要处理NULL成员*/
                        (byte) (IEntityMemberWriter.SF_STORE | IEntityMemberWriter.SF_WRITE_NULL));
            }
        }
    }
}
