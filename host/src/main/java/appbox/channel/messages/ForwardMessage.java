package appbox.channel.messages;

import appbox.channel.IClientMessage;
import appbox.channel.IMessage;
import appbox.channel.MessageType;
import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;

/** 请求主进程转发给前端的消息 */
public final class ForwardMessage implements IMessage {

    private final long           sessionId;
    private final byte           msgType;
    private final IClientMessage target; //转发给前端的消息

    public ForwardMessage(long sessionId, byte msgType, IClientMessage target) {
        this.sessionId = sessionId;
        this.msgType   = msgType;
        this.target    = target;
    }

    @Override
    public byte MessageType() {
        return MessageType.ForwardMessage;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeLong(sessionId);
        bs.writeByte(msgType);
        //不要调整以上顺序，跟前端格式一致
        target.writeTo(bs);
    }

    @Override
    public void readFrom(IInputStream bs) {
        throw new UnsupportedOperationException();
    }
}
