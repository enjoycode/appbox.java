package appbox.channel.messages;

import appbox.channel.KVRowReader;
import appbox.data.EntityId;
import appbox.model.entity.SysIndexModel;
import appbox.serialization.BinSerializer;
import appbox.serialization.IOutputStream;
import appbox.store.KVTxnId;
import appbox.store.KeyUtil;
import appbox.utils.IdUtil;

public final class KVDeleteIndexRequest extends KVDeleteRequest {

    private final EntityId      _entityId;
    private final SysIndexModel _indexModel;
    private final byte[]        _stored;        //删除实体时返回的现存储的版本

    public KVDeleteIndexRequest(KVTxnId txnId, EntityId entityId, byte[] stored, SysIndexModel indexModel) {
        super(txnId);

        _entityId   = entityId;
        _indexModel = indexModel;
        _stored     = stored;

        raftGroupId   = entityId.raftGroupId();
        schemaVersion = indexModel.owner.sysStoreOptions().schemaVersion();
        returnExists  = false;
        dataCF        = KeyUtil.INDEXCF_INDEX;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        super.writeTo(bs);

        //refs always 0
        bs.writeVariant(0);

        //key
        //写入EntityId's RaftGroupId
        _entityId.writePart1(bs);
        //写入IndexId
        bs.writeByte(_indexModel.indexId());
        //写入各字段, 注意MemberId写入排序标记
        for (var field : _indexModel.fields()) {
            //惟一索引但字段不具备值的处理, 暂 id | null flag
            var flags = field.orderByDesc ? (1 << IdUtil.MEMBERID_ORDER_OFFSET) : 0;
            KVRowReader.writeMember(_stored, field.memberId, bs, (byte) flags); //暂只需要OrderByFlag
        }
        //写入非惟一索引的EntityId的第二部分
        if (!_indexModel.unique()) {
            _entityId.writePart2(bs);
        }
    }
}
