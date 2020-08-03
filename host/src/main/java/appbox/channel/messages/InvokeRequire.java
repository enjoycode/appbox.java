package appbox.channel.messages;

import appbox.core.cache.ObjectPool;
import appbox.core.serialization.BinDeserializer;
import appbox.core.serialization.BinSerializer;

public final class InvokeRequire implements IMessage {
    //TODO: pool count
    public static final ObjectPool<InvokeRequire> pool = new ObjectPool<>(InvokeRequire::new, null, 32);

    public int reqId; // 不用序列化

    /**
     * 请求所在的主进程的shard
     */
    public short shard;

    /**
     * eg: sys.OrderService.Save
     */
    public String service;

    private InvokeRequire() {
    }

    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        throw new Exception();
    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {
        shard = bs.readShort();
        //TODO:读取原始请求
    }
}
