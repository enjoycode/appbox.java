package appbox.server.channel.messages;

import appbox.core.serialization.IBinSerializable;

public interface IMessage extends IBinSerializable {
    byte MessageType();
}
