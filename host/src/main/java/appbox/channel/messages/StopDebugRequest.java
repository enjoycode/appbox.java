package appbox.channel.messages;

import appbox.channel.IMessage;
import appbox.channel.MessageType;
import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;

public final class StopDebugRequest implements IMessage {

    public long   sessionId;
    private byte[] invokeResponse;

    public StopDebugRequest() {}

    public StopDebugRequest(long debugSessionId) {
        sessionId      = debugSessionId;
        invokeResponse = null;
    }

    @Override
    public byte MessageType() {
        return MessageType.StopDebuggerRequest;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeLong(sessionId);
    }

    @Override
    public void readFrom(IInputStream bs) {
        sessionId      = bs.readLong();
        invokeResponse = bs.readRemaining();
    }
}
