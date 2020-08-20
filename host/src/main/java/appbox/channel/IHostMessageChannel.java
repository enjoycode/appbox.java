package appbox.channel;

import appbox.serialization.BinDeserializer;
import com.sun.jna.Pointer;

/**
 * 服务端主子进程通讯通道接口
 */
public interface IHostMessageChannel extends IMessageChannel {

    /**
     * 归还或释放完整的消息缓存
     */
    void returnAllChunks(Pointer first);

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
