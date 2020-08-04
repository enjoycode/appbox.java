package appbox.channel;

import appbox.channel.messages.IMessage;
import appbox.core.cache.ObjectPool;
import appbox.core.serialization.BinDeserializer;
import com.sun.jna.Pointer;

/**
 * 消息序列化辅助
 */
public final class MessageSerializer {

    /**
     * 反序列化至指定类型的消息
     *
     * @param msg
     * @param first
     * @param <T>
     */
    public static <T extends IMessage> void deserialize(T msg, Pointer first) throws Exception {
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
