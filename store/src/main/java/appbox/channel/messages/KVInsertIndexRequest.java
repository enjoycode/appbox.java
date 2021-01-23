package appbox.channel.messages;

import appbox.data.SysEntity;
import appbox.model.entity.SysIndexModel;
import appbox.serialization.IOutputStream;
import appbox.store.KVTxnId;
import appbox.store.KVUtil;

public final class KVInsertIndexRequest extends KVInsertRequire {

    private final SysEntity     _entity;
    private final SysIndexModel _indexModel;

    public KVInsertIndexRequest(KVTxnId txnId, SysEntity entity, SysIndexModel indexModel) {
        super(txnId);

        _entity     = entity;
        _indexModel = indexModel;

        raftGroupId    = entity.id().raftGroupId();
        schemaVersion  = indexModel.owner.sysStoreOptions().schemaVersion();
        overrideExists = false;
        dataCF         = KVUtil.INDEXCF_INDEX;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        super.writeTo(bs);

        //key(需要长度信息)
        KVUtil.writeIndexKey(bs, _indexModel, _entity, true);
        //refs always 0
        bs.writeVariant(0);
        //data(不需要长度信息)
        KVUtil.writeIndexValue(bs, _indexModel, _entity);
    }
}
