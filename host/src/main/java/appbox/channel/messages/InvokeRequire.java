package appbox.channel.messages;

import appbox.cache.ObjectPool;
import appbox.channel.IMessage;
import appbox.runtime.InvokeArg;
import appbox.channel.MessageType;
import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;

import java.util.ArrayList;

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
    public       long                 sessionId;//当前会话标识，无则=0
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

    public void addArg(String value) {
        var arg = InvokeArg.pool.rent();
        arg.setValue(value);
        args.add(arg);
    }

    private void clearArgs() {
        for (InvokeArg arg : args) {
            InvokeArg.pool.back(arg);
        }
        args.clear();
    }

    //region ====Serialization====
    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeShort(shard);
        bs.writeLong(sessionId);
        bs.writeString(service);
        bs.writeVariant(args.size());
        for (InvokeArg arg : args) {
            arg.writeTo(bs);
        }
    }

    @Override
    public void readFrom(IInputStream bs) {
        shard     = bs.readShort();
        sessionId = bs.readLong();
        service   = bs.readString();
        int argsLen = bs.readVariant();
        for (int i = 0; i < argsLen; i++) {
            var arg = InvokeArg.pool.rent();
            arg.readFrom(bs);
            args.add(arg);
        }
    }
    //endregion
}
