package appbox.channel.messages;

import appbox.cache.ObjectPool;
import appbox.channel.IMessage;
import appbox.channel.MessageType;
import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;

public final class InvokeResponse implements IMessage {
    public static final class ErrorCode {
        public static final byte None                   = 0;
        public static final byte DeserializeRequestFail = 1;
        public static final byte ServiceNotExists       = 2;
        public static final byte ServiceInnerError      = 3;
        public static final byte SessionNotExists       = 4;
        public static final byte SerializeResponseFail  = 5;
        public static final byte Timeout                = 6;
    }

    private static final ObjectPool<InvokeResponse> pool = new ObjectPool<>(InvokeResponse::new, 32);

    public static InvokeResponse rentFromPool() {
        return pool.rent();
    }

    public static void backToPool(InvokeResponse obj) {
        pool.back(obj);
    }

    public int    reqId;
    public short  shard;
    public byte   error;    //无错误=0
    public Object result;   //有错误则为错误信息

    @Override
    public byte MessageType() {
        return MessageType.InvokeResponse;
    }

    //region ====Serialization====
    @Override
    public void writeTo(IOutputStream bs) {
        //注意不要改变写入顺序
        bs.writeInt(reqId);
        bs.writeShort(shard);
        bs.writeByte(error);
        bs.serialize(result);
    }

    @Override
    public void readFrom(IInputStream bs) {
        reqId  = bs.readInt();
        shard  = bs.readShort();
        error  = bs.readByte();
        result = bs.deserialize();
    }
    //endregion
}
