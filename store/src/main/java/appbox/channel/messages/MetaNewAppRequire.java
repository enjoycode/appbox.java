package appbox.channel.messages;

import appbox.channel.IMessage;
import appbox.model.ApplicationModel;
import appbox.serialization.BinDeserializer;
import appbox.serialization.BinSerializer;
import appbox.channel.MessageType;
import appbox.store.KeyUtil;

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
    public void writeTo(BinSerializer bs) {
        //写入５字节Key
        KeyUtil.writeAppKey(bs, application.id(), false);
        //写入模型数据,注意不需要写入头部9字节，由读取端处理
        application.writeTo(bs);
    }

    @Override
    public void readFrom(BinDeserializer bs) {

    }
    //endregion
}