package appbox.channel.messages;

import appbox.channel.IMessage;
import appbox.model.ApplicationModel;
import appbox.channel.MessageType;
import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;
import appbox.store.KVUtil;

public final class MetaNewAppRequire implements IMessage {

    public final ApplicationModel application;

    public MetaNewAppRequire(ApplicationModel app) {
        application = app;
    }

    @Override
    public byte MessageType() {
        return MessageType.MetaNewAppRequire;
    }

    //region ====Serialization====
    @Override
    public void writeTo(IOutputStream bs) {
        //写入５字节Key
        KVUtil.writeAppKey(bs, application.id(), false);
        //写入模型数据,注意不需要写入头部9字节，由读取端处理
        application.writeTo(bs);
    }

    @Override
    public void readFrom(IInputStream bs) {

    }
    //endregion
}
