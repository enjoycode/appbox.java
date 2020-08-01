package appbox.channel;

import appbox.channel.messages.IMessage;
import appbox.core.cache.ObjectPool;
import com.sun.jna.Pointer;

/**
 * 消息序列化器
 */
public final class MessageSerializer {

    private static final ObjectPool<MessageSerializer> pool = new ObjectPool<>(MessageSerializer::new, null, 32);

    private final MessageReadStream _stream;

    private MessageSerializer() {
        _stream = new MessageReadStream();
    }

    /**
     * 反序列化至指定类型的消息
     *
     * @param msg
     * @param first
     * @param <T>
     */
    public static <T extends IMessage> void deserialize(T msg, Pointer first) throws Exception {
        var serializer = pool.rent();
        serializer.reset(first);
        try {
            msg.readFrom(serializer);
        } finally {
            pool.back(serializer);
        }
    }

    private void reset(Pointer first) {
        _stream.reset(first);
    }

    public short readShort() throws Exception {
        return _stream.readShort();
    }
}
