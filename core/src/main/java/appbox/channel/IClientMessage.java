package appbox.channel;

import appbox.serialization.IOutputStream;

/** 用于转发给前端的消息 */
public interface IClientMessage {

    void writeTo(IOutputStream bs);

}
