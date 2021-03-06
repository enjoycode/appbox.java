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

public final class KVInsertEntityRequest extends KVInsertRequire {

    private final SysEntity            _entity;
    private final EntityModel          _model;
    private final List<EntityRefModel> _refsWithFK;

    public KVInsertEntityRequest(SysEntity entity, EntityModel model,
                                 List<EntityRefModel> refsWithFK, KVTxnId txnId) {
        super(txnId);

        dataCF        = -1;
        raftGroupId   = entity.id().raftGroupId();
        schemaVersion = model.sysStoreOptions().schemaVersion();

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
        //data
        for (var m : _model.getMembers()) {
            if (m.type() == EntityMemberModel.EntityMemberType.DataField) {
                _entity.writeMember(m.memberId(), bs, IEntityMemberWriter.SF_STORE);
            }
        }
    }

}
