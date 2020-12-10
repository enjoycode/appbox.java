package appbox.channel.messages;

import appbox.channel.MessageType;
import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;

/**
 * 通用的存储命令响应
 */
public final class KVCommandResponse extends StoreResponse {

    private byte[] results;

    public byte[] getResults() { return results; }

    @Override
    public byte MessageType() {
        return MessageType.KVCommandResponse;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readFrom(IInputStream bs) {
        reqId     = bs.readInt();
        errorCode = bs.readInt();
        if (errorCode == 0) {
            results = bs.readRemaining();
        }
    }
}
