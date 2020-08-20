package appbox.channel;

public interface IMessageChannel {

    /**
     * 生成新的消息标识
     */
    int newMessageId();

    /**
     * 序列化并发送消息
     */
    <T extends IMessage> void sendMessage(int id, T msg) throws Exception;

}
