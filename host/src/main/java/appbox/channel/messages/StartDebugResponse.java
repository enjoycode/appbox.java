package appbox.channel.messages;

import appbox.channel.IMessage;
import appbox.channel.MessageType;
import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;

public final class StartDebugResponse implements IMessage {
    public long    sessionId;
    public boolean ok;

    @Override
    public byte MessageType() {
        return MessageType.StartDebuggerResponse;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readFrom(IInputStream bs) {
        sessionId = bs.readLong();
        ok        = bs.readBool();
    }
}
