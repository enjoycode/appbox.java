package appbox.channel.messages;

import appbox.core.cache.ObjectPool;
import appbox.core.serialization.BinDeserializer;
import appbox.core.serialization.BinSerializer;

public final class InvokeResponse implements IMessage {
    private static final ObjectPool<InvokeResponse> pool = new ObjectPool<>(InvokeResponse::new, null, 32);

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
