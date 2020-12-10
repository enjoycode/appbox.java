package appbox.channel.messages;

import appbox.expressions.Expression;
import appbox.serialization.IOutputStream;
import appbox.store.KeyUtil;

/** 扫描表分区请求 */
public final class KVScanEntityRequest extends KVScanRequest {
    private final long       raftGroupId;
    private final int        skip;
    private final int        take;
    private final Expression filter;
    //TODO:1.事务标识或只读事务的时间戳;2.CreateTime谓词定位

    public KVScanEntityRequest(long raftGroupId, int skip, int take, Expression filter) {
        this.raftGroupId = raftGroupId;
        this.skip        = skip;
        this.take        = take;
        this.filter      = filter;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeInt(0); //ReqId占位
        bs.writeLong(raftGroupId); //raftGroupId

        //暂没有CreateTime谓词使用前缀匹配方式
        bs.writeNativeVariant(KeyUtil.ENTITY_KEY_SIZE - 10); //BeginKeySize
        //写入分区前缀，与EntityId.initRaftGroupId一致
        KeyUtil.writeRaftGroupId(bs, raftGroupId);

        bs.writeNativeVariant(0); //EndKeySize

        bs.writeInt(skip); //Skip
        bs.writeInt(take); //Take
        bs.writeLong(0); //Timestamp
        bs.writeByte((byte) -1); //DataCF
        bs.writeBool(false); //IsMVCC TODO: remove it
        bs.writeBool(false); //ToIndexTarget

        //Filter (最后)如果有则写入
        if (filter != null) {
            bs.serialize(filter);
        }
    }
}
