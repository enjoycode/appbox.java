package appbox.channel.messages;

import appbox.core.cache.ObjectPool;
import appbox.core.data.InvokeArg;
import appbox.core.serialization.BinDeserializer;
import appbox.core.serialization.BinSerializer;

import java.util.ArrayList;

/**
 * 主进程转发的服务调用请求，包含主进程的相关信息及原始请求信息
 */
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

    //TODO: srcId //原客户端请求的标识，http始终为0，websocket区分
    public        int                  reqId;    //主进程转发的消息id,用于等待子进程处理完. 不用序列化
    public        short                shard;    //主进程的shard
    public        String               service;  //eg: sys.OrderService.Save
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
        //注意：目前仅用于测试，不写入主进程相关信息(相当于原始客户端请求)
        bs.writeString(service);
        bs.writeVariant(args.size());
        for (int i = 0; i < args.size(); i++) {
            args.get(i).writeTo(bs);
        }
    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {
        shard   = bs.readShort();
        service = bs.readString();
        int argsLen = bs.readVariant();
        for (int i = 0; i < argsLen; i++) {
            var arg = InvokeArg.pool.rent();
            arg.readFrom(bs);
            args.add(arg);
        }
    }
}
