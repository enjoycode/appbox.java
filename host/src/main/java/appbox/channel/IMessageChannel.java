package appbox.channel;

import com.sun.jna.Pointer;

public interface IMessageChannel {

    /**
     * 归还或释放完整的消息缓存
     */
    void returnAllChunks(Pointer first);

}
