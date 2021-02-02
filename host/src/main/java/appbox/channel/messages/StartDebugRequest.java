package appbox.channel.messages;

import appbox.channel.IMessage;
import appbox.channel.MessageType;
import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;

/** 通知主进程准备好调试通道，并发送调用目标服务的请求 */
public final class StartDebugRequest implements IMessage {

    private final long   sessionId;
    private final String service; //eg: sys.OrderService.save
    private final byte[] invokeArgs;

    public StartDebugRequest(long sessionId, String service, byte[] invokeArgs) {
        this.sessionId  = sessionId;
        this.service    = service;
        this.invokeArgs = invokeArgs;
    }

    @Override
    public byte MessageType() {
        return MessageType.StartDebuggerRequest;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeLong(sessionId);
        bs.writeNativeString(service);
        if (invokeArgs == null) {
            bs.writeNativeVariant(0);
        } else {
            bs.writeNativeVariant(invokeArgs.length);
            bs.write(invokeArgs, 0, invokeArgs.length);
        }
    }

    @Override
    public void readFrom(IInputStream bs) {
        throw new UnsupportedOperationException();
    }

}
