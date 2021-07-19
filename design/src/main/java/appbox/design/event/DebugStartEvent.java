package appbox.design.event;

import appbox.channel.IClientMessage;
import appbox.design.IDeveloperSession;
import appbox.serialization.IOutputStream;

/** 发送给前端的调试器已启动事件 */
public final class DebugStartEvent implements IClientMessage {

    public static final DebugStartEvent Default = new DebugStartEvent();

    private DebugStartEvent() {}

    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeInt(IDeveloperSession.DEBUG_EVENT);
        bs.writeByte(DebugEventType.START);
    }
}
