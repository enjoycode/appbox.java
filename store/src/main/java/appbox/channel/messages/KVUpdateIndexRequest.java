package appbox.channel.messages;

import appbox.data.SysEntity;
import appbox.model.entity.SysIndexModel;
import appbox.serialization.IOutputStream;
import appbox.store.KVTxnId;
import appbox.store.KVUtil;

/** 仅适用于更新索引的StoringFields, 即IndexKey没有变化 */
public final class KVUpdateIndexRequest extends KVUpdateRequest {

    private final SysEntity     _entity;
    private final SysIndexModel _indexModel;

    public KVUpdateIndexRequest(KVTxnId txnId, SysEntity entity, SysIndexModel indexModel) {
        super(txnId);

        dataCF        = KVUtil.INDEXCF_INDEX;
        raftGroupId   = entity.id().raftGroupId();
        schemaVersion = indexModel.owner.sysStoreOptions().schemaVersion();
        merge         = false;
        returnExists  = false;

        _entity     = entity;
        _indexModel = indexModel;
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
