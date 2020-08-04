package appbox.channel.messages;

import appbox.core.cache.ObjectPool;
import appbox.core.data.InvokeArg;
import appbox.core.serialization.BinDeserializer;
import appbox.core.serialization.BinSerializer;

import java.util.ArrayList;

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

    private ArrayList<InvokeArg> args;

    private InvokeRequire() {
        args = new ArrayList<>();
    }

    public void addArg(int value) {
        var arg = InvokeArg.pool.rent();
        arg.setValue(value);
        args.add(arg);
    }

    public void clearArgs() {
        for (int i = 0; i < args.size() - 1; i++) {
            InvokeArg.pool.back(args.get(i));
        }
        args.clear();
    }

    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        throw new Exception();
    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {
        shard = bs.readShort();
        //TODO:读取原始请求
        service = bs.readString();
        int argsLen = bs.readVariant();
    }
}
