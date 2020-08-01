package appbox.channel.messages;

import appbox.channel.MessageSerializer;

public interface IMessage {

    public void readFrom(MessageSerializer serializer) throws Exception;

}
