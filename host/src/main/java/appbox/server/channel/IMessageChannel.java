package appbox.server.channel;

import appbox.server.channel.messages.IMessage;
import appbox.serialization.BinDeserializer;
import com.sun.jna.Pointer;

public interface IMessageChannel {

    /**
     * 归还或释放完整的消息缓存
     */
    void returnAllChunks(Pointer first);

    /**
     * 生成新的消息标识
     */
    int newMessageId();

    /**
     * 序列化并发送消息
     */
    <T extends IMessage> void sendMessage(int id, T msg) throws Exception;

    /**
     * 反序列化至指定类型的消息，注意消息缓存块由调用者释放
     */
    static <T extends IMessage> void deserialize(T msg, Pointer first) throws Exception { //TODO: NO Exception
        var stream = MessageReadStream.rentFromPool(first);
        var reader = BinDeserializer.rentFromPool(stream);
        try {
            msg.readFrom(reader);
        } finally {
            BinDeserializer.backToPool(reader);
            MessageReadStream.backToPool(stream);
        }
    }

}
