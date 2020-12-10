package appbox.channel.messages;

import appbox.channel.MemberSizeCounter;
import appbox.data.SysEntity;
import appbox.model.entity.SysIndexModel;
import appbox.serialization.BinSerializer;
import appbox.serialization.IEntityMemberWriter;
import appbox.serialization.IOutputStream;
import appbox.store.KVTxnId;
import appbox.store.KeyUtil;
import appbox.utils.IdUtil;

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
        dataCF         = KeyUtil.INDEXCF_INDEX;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        super.writeTo(bs);

        //key(需要长度信息)
        //先计算并写入Key长度
        var sizeCounter = new MemberSizeCounter();
        for (var field : _indexModel.fields()) {
            _entity.writeMember(field.memberId, sizeCounter, IEntityMemberWriter.SF_NONE); //flags无意义
        }
        bs.writeNativeVariant(6 + 1 + sizeCounter.getSize());
        //写入EntityId's RaftGroupId
        _entity.id().writePart1(bs);
        //写入IndexId
        bs.writeByte(_indexModel.indexId());
        //写入各字段, 注意MemberId写入排序标记
        for (var field : _indexModel.fields()) {
            //惟一索引但字段不具备值的处理, 暂 id | null flag
            byte flags = IEntityMemberWriter.SF_STORE | IEntityMemberWriter.SF_WRITE_NULL;
            if (field.orderByDesc)
                flags |= IEntityMemberWriter.SF_ORDER_BY_DESC;
            _entity.writeMember(field.memberId, bs, flags);
        }
        //写入非惟一索引的EntityId的第二部分
        if (!_indexModel.unique()) {
            _entity.id().writePart2(bs);
        }

        //refs always 0
        bs.writeVariant(0);

        //data(不需要长度信息)
        //先写入惟一索引指向的EntityId
        if (_indexModel.unique()) {
            bs.writeShort(IdUtil.STORE_FIELD_ID_OF_ENTITY_ID);
            _entity.id().writeTo(bs);
        }
        //再写入StoringFields
        if (_indexModel.hasStoringFields()) {
            for (var sf : _indexModel.storingFields()) {
                _entity.writeMember(sf, bs, IEntityMemberWriter.SF_STORE); //暂不写入null成员
            }
        }

    }
}
