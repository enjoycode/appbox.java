package appbox.design.services.debug;

import appbox.channel.IClientMessage;
import appbox.design.IDeveloperSession;
import appbox.serialization.IOutputStream;

/** 发送给前端的调试器已停止事件 */
final class DebugStopEvent implements IClientMessage {

    public static final DebugStopEvent Default = new DebugStopEvent();

    private DebugStopEvent() {}

    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeInt(IDeveloperSession.DEBUG_EVENT);
        bs.writeByte(DebugEventType.STOP);
    }

}
