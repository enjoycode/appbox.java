package appbox.channel.messages;

import appbox.data.EntityId;
import appbox.model.EntityModel;
import appbox.model.entity.EntityRefModel;
import appbox.serialization.IOutputStream;
import appbox.store.KVTxnId;
import appbox.store.KeyUtil;

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

        //refs 暂存储层是字符串，实际是short数组
        int refsSize = 0;
        if (_refsWithFK != null && _refsWithFK.size() > 0) {
            refsSize = _refsWithFK.size() * 2;
        }
        bs.writeNativeVariant(refsSize);
        if (refsSize > 0) {
            for (var r : _refsWithFK) {
                bs.writeShort(r.getFKMemberIds()[0]);
            }
        }

        //key 不带长度信息
        KeyUtil.writeEntityKey(bs, _id, false);
    }
}
