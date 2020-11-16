package appbox.channel.messages;

import appbox.data.SysEntity;
import appbox.model.EntityModel;
import appbox.serialization.BinSerializer;
import appbox.serialization.IEntityMemberWriter;
import appbox.store.KVTxnId;
import appbox.store.KeyUtil;

public final class KVInsertEntityRequest extends KVInsertRequire {

    private final SysEntity   _entity;
    private final EntityModel _model;

    public KVInsertEntityRequest(SysEntity entity, EntityModel model, KVTxnId txnId) {
        super(txnId);

        dataCF        = -1;
        raftGroupId   = entity.id().raftGroupId();
        schemaVersion = model.sysStoreOptions().schemaVersion();

        _entity = entity;
        _model  = model;
    }

    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        super.writeTo(bs);

        //key
        KeyUtil.writeEntityKey(bs, _entity.id(), true);
        //refs //TODO:
        bs.writeVariant(0);
        //data
        for (var m : _model.getMembers()) {
            _entity.writeMember(m.memberId(), bs, IEntityMemberWriter.SF_STORE);
        }
    }

}
