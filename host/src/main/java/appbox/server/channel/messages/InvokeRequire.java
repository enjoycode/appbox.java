package appbox.server.channel.messages;

import appbox.core.cache.ObjectPool;
import appbox.core.runtime.InvokeArg;
import appbox.core.serialization.BinDeserializer;
import appbox.core.serialization.BinSerializer;
import appbox.server.channel.MessageType;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * 主进程转发的服务调用请求，包含主进程的相关信息及原始请求信息
 */
public final class InvokeRequire implements IMessage {
    //region ====ObjectPool====
    //TODO: pool count
    private static final ObjectPool<InvokeRequire> pool = new ObjectPool<>(InvokeRequire::new, 32);

    public static InvokeRequire rentFromPool() {
        return pool.rent();
    }

    public static void backToPool(InvokeRequire obj) {
        obj.clearArgs();
        pool.back(obj);
    }
    //endregion

    //TODO: srcId //原客户端请求的标识，http始终为0，websocket区分
    public       int                  reqId;    //主进程转发的消息id,用于等待子进程处理完. 不用序列化
    public       short                shard;    //主进程的shard
    public       String               service;  //eg: sys.OrderService.Save
    public final ArrayList<InvokeArg> args;

    private InvokeRequire() {
        args = new ArrayList<>();
    }

    @Override
    public byte MessageType() {
        return MessageType.InvokeRequire;
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

    private void clearArgs() {
        for (int i = 0; i < args.size(); i++) {
            InvokeArg.pool.back(args.get(i));
        }
        args.clear();
    }

    //region ====Serialization====
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
        shard   = bs.readShort();
        service = bs.readString();
        int argsLen = bs.readVariant();
        for (int i = 0; i < argsLen; i++) {
            var arg = InvokeArg.pool.rent();
            arg.readFrom(bs);
            args.add(arg);
        }
    }
    //endregion
}
