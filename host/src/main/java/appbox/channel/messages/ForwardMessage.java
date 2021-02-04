package appbox.channel.messages;

import appbox.channel.IMessage;
import appbox.channel.MessageType;
import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;

/** 请求主进程转发给前端的消息 */
public final class ForwardMessage implements IMessage {

    private final long   sessionId;
    private final byte   msgType;
    private final int    msgId;
    private final String body;

    public ForwardMessage(long sessionId, byte msgType, int msgId, String body) {
        this.sessionId = sessionId;
        this.msgType   = msgType;
        this.msgId     = msgId;
        this.body      = body;
    }

    @Override
    public byte MessageType() {
        return MessageType.ForwardMessage;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        //TODO:暂按Event格式写入
        bs.writeLong(sessionId);
        bs.writeByte(msgType);
        bs.writeInt(msgId);
        bs.writeUtf8(body);
    }

    @Override
    public void readFrom(IInputStream bs) {
        throw new UnsupportedOperationException();
    }
}
