package appbox.channel.messages;

import appbox.channel.IMessage;
import appbox.channel.MessageType;
import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;

public final class BlobCommandRequest implements IMessage {
    private final long   raftGroupId;
    private final byte   action;
    private final int    size;
    private final int    option1;
    private final int    option2;
    private final long   objectId;
    private final String path;

    public BlobCommandRequest(long raftGroupId, byte action, int size, int option1, int option2,
                              long objectId, String path) {
        this.raftGroupId = raftGroupId;
        this.action      = action;
        this.size        = size;
        this.option1     = option1;
        this.option2     = option2;
        this.objectId    = objectId;
        this.path        = path;
    }

    @Override
    public byte MessageType() {
        return MessageType.BlobCommandRequest;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeLong(raftGroupId);
        bs.writeByte(action);
        bs.writeInt(size);
        bs.writeInt(option1);
        bs.writeInt(option2);
        bs.writeLong(objectId);
        bs.writeNativeString(path);
    }

    @Override
    public void readFrom(IInputStream bs) {
        throw new UnsupportedOperationException();
    }
}
