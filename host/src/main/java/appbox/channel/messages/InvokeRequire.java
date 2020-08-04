package appbox.channel.messages;

import appbox.core.cache.ObjectPool;
import appbox.core.data.InvokeArg;
import appbox.core.serialization.BinDeserializer;
import appbox.core.serialization.BinSerializer;

import java.util.ArrayList;

public final class InvokeRequire implements IMessage {
    //TODO: pool count
    private static final ObjectPool<InvokeRequire> pool = new ObjectPool<>(InvokeRequire::new, null, 32);

    public static InvokeRequire rentFromPool() {
        return pool.rent();
    }

    public static void backToPool(InvokeRequire obj) {
        obj.clearArgs();
        pool.back(obj);
    }

    public int reqId; // 不用序列化

    /**
     * 请求所在的主进程的shard
     */
    public short shard;

    /**
     * eg: sys.OrderService.Save
     */
    public String service;

    private final ArrayList<InvokeArg> args;

    private InvokeRequire() {
        args = new ArrayList<>();
    }

    public int getArgsCount() {
        return args.size();
    }

    public InvokeArg getArg(int index) {
        return args.get(index);
    }

    public void addArg(int value) {
        var arg = InvokeArg.pool.rent();
        arg.setValue(value);
        args.add(arg);
    }

    public void clearArgs() {
        for (int i = 0; i < args.size(); i++) {
            InvokeArg.pool.back(args.get(i));
        }
        args.clear();
    }

    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        bs.writeShort(shard);
        bs.writeString(service);
        bs.writeVariant(args.size());
        for (int i = 0; i < args.size(); i++) {
            args.get(i).writeTo(bs);
        }
    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {
        shard = bs.readShort();
        service = bs.readString();
        int argsLen = bs.readVariant();
        for (int i = 0; i < argsLen; i++) {
            var arg = InvokeArg.pool.rent();
            arg.readFrom(bs);
            args.add(arg);
        }
    }
}
