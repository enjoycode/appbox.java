package appbox.channel;

import com.sun.jna.Pointer;

/** 服务端主子进程通讯通道接口 */
public interface IHostMessageChannel extends IMessageChannel {

    /** 归还或释放完整的消息缓存 */
    void returnAllChunks(Pointer first);

    void close();

    /** 反序列化至指定类型的消息，注意消息缓存块由调用者释放 */
    static <T extends IMessage> void deserialize(T msg, Pointer first) {
        var stream = MessageReadStream.rentFromPool(first);
        try {
            msg.readFrom(stream);
        } finally {
            MessageReadStream.backToPool(stream);
        }
    }

}
