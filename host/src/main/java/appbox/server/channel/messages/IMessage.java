package appbox.server.channel.messages;

import appbox.serialization.IBinSerializable;

public interface IMessage extends IBinSerializable {
    byte MessageType();
}
