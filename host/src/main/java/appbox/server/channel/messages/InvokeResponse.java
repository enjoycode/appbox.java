package appbox.server.channel.messages;

import appbox.core.cache.ObjectPool;
import appbox.core.serialization.BinDeserializer;
import appbox.core.serialization.BinSerializer;

public final class InvokeResponse implements IMessage {
    public static final class ErrorCode {
        public static final byte None                   = 0;
        public static final byte DeserializeRequestFail = 1;
        public static final byte ServiceNotExists       = 2;
        public static final byte ServiceInnerError      = 3;
        public static final byte SessionNotExists       = 4;
        public static final byte SerializeResponseFail  = 5;
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
    public byte   error;
    public Object result;

    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        //注意不要改变写入顺序
        bs.writeInt(reqId);
        bs.writeShort(shard);
        bs.writeByte(error);
        bs.serialize(result);
    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {
        reqId  = bs.readInt();
        shard  = bs.readShort();
        error  = bs.readByte();
        result = bs.deserialize();
    }
}
