package appbox.channel.messages;

import appbox.channel.MessageSerializer;
import appbox.core.cache.ObjectPool;

public final class InvokeRequire implements IMessage {
    //TODO: pool count
    public static final ObjectPool<InvokeRequire> pool = new ObjectPool<>(InvokeRequire::new, null, 32);

    public int reqId;

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
    public void readFrom(MessageSerializer serializer) throws Exception {
        shard = serializer.readShort();
        //TODO:读取原始请求
    }
}
