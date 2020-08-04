package appbox.channel;

import appbox.channel.messages.IMessage;
import appbox.core.serialization.BinDeserializer;
import com.sun.jna.Pointer;

public interface IMessageChannel {

    /**
     * 归还或释放完整的消息缓存
     */
    void returnAllChunks(Pointer first);


    /**
     * 反序列化至指定类型的消息，注意消息缓存块由调用者释放
     *
     * @param msg
     * @param first
     * @param <T>
     */
    static <T extends IMessage> void deserialize(T msg, Pointer first) throws Exception {
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
