package appbox.channel.messages;

import appbox.expressions.Expression;
import appbox.serialization.BinSerializer;
import appbox.store.KeyUtil;

/** 扫描表分区请求 */
public final class KVScanTableRequest extends KVScanRequest {
    private final long       raftGroupId;
    private final int        skip;
    private final int        take;
    private final Expression filter;

    public KVScanTableRequest(long raftGroupId, int skip, int take, Expression filter) {
        this.raftGroupId = raftGroupId;
        this.skip        = skip;
        this.take        = take;
        this.filter      = filter;
    }

    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        bs.writeInt(0); //ReqId占位
        bs.writeLong(raftGroupId); //raftGroupId

        //暂没有CreateTime谓词使用前缀匹配方式
        bs.writeNativeVariant(KeyUtil.ENTITY_KEY_SIZE - 10); //BeginKeySize
        //写入分区前缀，与EntityId.initRaftGroupId一致
        //前32位
        int p1 = (int) (raftGroupId >>> 12);
        bs.writeByte((byte) (p1 & 0xFF));
        bs.writeByte((byte) (p1 >>> 8));
        bs.writeByte((byte) (p1 >>> 16));
        bs.writeByte((byte) (p1 >>> 24));
        //后12位　<< 4
        short p2 = (short) ((raftGroupId & 0xFFF) << 4);
        bs.writeByte((byte) (p2 & 0xFF));
        bs.writeByte((byte) (p2 >>> 8));

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
