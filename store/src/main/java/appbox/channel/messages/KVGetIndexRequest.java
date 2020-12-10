package appbox.channel.messages;

import appbox.expressions.KVFieldExpression;
import appbox.model.entity.SysIndexModel;
import appbox.serialization.IEntityMemberWriter;
import appbox.serialization.IOutputStream;
import appbox.store.KeyUtil;

public final class KVGetIndexRequest extends KVGetRequest {

    private final long                _raftGroupId;
    private final SysIndexModel       _indexModel;
    private final KVFieldExpression[] _fields;
    private final Object[]            _values;

    public KVGetIndexRequest(long raftGroupId, SysIndexModel indexModel,
                             KVFieldExpression[] fields, Object[] values) {
        _raftGroupId = raftGroupId;
        _indexModel  = indexModel;
        _fields      = fields;
        _values      = values;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeInt(0);                     //ReqId占位
        bs.writeLong(_raftGroupId);               //raftGroupId
        bs.writeByte(KeyUtil.INDEXCF_INDEX);      //dataCF
        bs.writeLong(0);                    //timestamp

        //最后写入不带长度信息的Key
        KeyUtil.writeRaftGroupId(bs, _raftGroupId); //key's EntityId's part1
        bs.writeByte(_indexModel.indexId());        //key's index id
        //写入各字段值,注意MemberId写入排序标记
        for (int i = 0; i < _indexModel.fields().length; i++) {
            byte flags = IEntityMemberWriter.SF_STORE | IEntityMemberWriter.SF_WRITE_NULL;
            if (_indexModel.fields()[i].orderByDesc)
                flags |= IEntityMemberWriter.SF_ORDER_BY_DESC;
            switch (_fields[i].fieldType){
                case String:
                    bs.writeMember(_fields[i].fieldId, this.<String>getValue(i), flags); break;
                case Int:
                    bs.writeMember(_fields[i].fieldId, this.<Integer>getValue(i), flags); break;
                default:
                    throw new RuntimeException("未实现"); //TODO: others
            }
        }

        //暂目前仅惟一索引，无需写入EntityId's part2
    }

    private <T> T getValue(int index) {
        if (_values[index] == null)
            return null;
        return (T) _values[index];
    }

}
