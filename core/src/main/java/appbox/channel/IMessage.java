package appbox.channel;

import appbox.serialization.IBinSerializable;

public interface IMessage extends IBinSerializable {
    byte MessageType();
}
