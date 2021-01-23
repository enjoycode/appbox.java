package appbox.channel.messages;

import appbox.data.EntityId;
import appbox.model.EntityModel;
import appbox.model.entity.EntityRefModel;
import appbox.serialization.IOutputStream;
import appbox.store.KVTxnId;
import appbox.store.KVUtil;

import java.util.List;

public final class KVDeleteEntityRequest extends KVDeleteRequest {

    private final EntityId             _id;
    private final List<EntityRefModel> _refsWithFK;

    public KVDeleteEntityRequest(KVTxnId txnId, EntityId id, EntityModel model,
                                 List<EntityRefModel> refsWithFK) {
        super(txnId);

        _id         = id;
        _refsWithFK = refsWithFK;

        raftGroupId   = id.raftGroupId();
        schemaVersion = model.sysStoreOptions().schemaVersion();
        returnExists  = model.sysStoreOptions().hasIndexes() || _refsWithFK != null;
        dataCF        = -1;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        super.writeTo(bs);

        //refs
        KVUtil.writeEntityRefs(bs, _refsWithFK);
        //key 不带长度信息
        KVUtil.writeEntityKey(bs, _id, false);
    }
}
