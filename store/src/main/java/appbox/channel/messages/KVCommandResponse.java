package appbox.channel.messages;

import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.channel.MessageType;

/**
 * 通用的无返回数据的存储命令响应
 */
public final class KVCommandResponse extends StoreResponse {

    @Override
    public byte MessageType() {
        return MessageType.KVCommandResponse;
    }

    @Override
    public void writeTo(BinSerializer bs) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readFrom(BinDeserializer bs) throws Exception {
        reqId     = bs.readInt();
        errorCode = bs.readInt();
    }
}
