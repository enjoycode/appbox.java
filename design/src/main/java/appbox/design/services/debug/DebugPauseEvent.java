package appbox.design.services.debug;

import appbox.channel.IClientMessage;
import appbox.design.IDeveloperSession;
import appbox.serialization.IOutputStream;

final class DebugPauseEvent implements IClientMessage {

    private final long threadId;
    private final int  line;

    public DebugPauseEvent(long threadId, int line) {
        this.threadId = threadId;
        this.line     = line;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeInt(IDeveloperSession.DEBUG_EVENT);
        bs.writeByte(DebugEventType.HIT_BREAKPOINT); //TODO: fix

        bs.writeLong(threadId);
        bs.writeInt(line);
    }

}
